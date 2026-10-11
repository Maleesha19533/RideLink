package com.ridelink.farepayment.security;

import com.ridelink.farepayment.client.RideManagementClient;
import com.ridelink.farepayment.client.dto.RideSummaryResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Confirms the currently authenticated caller is the passenger on the given ride before they
 * can pay for it or receive a receipt for it. Admins bypass this check (they can act on a
 * passenger's behalf); the URL rules in SecurityConfig already keep drivers off these
 * endpoints entirely.
 *
 * Ownership can only be verified by asking Ride Management Service who the ride's passenger
 * is, so - like the other Ride Management-dependent checks in this service - this is skipped
 * when RIDE_MANAGEMENT_INTEGRATION_ENABLED=false (isolated testing before that service
 * exists/is reachable).
 */
@Component
public class RideOwnershipGuard {

    private final RideManagementClient rideManagementClient;

    public RideOwnershipGuard(RideManagementClient rideManagementClient) {
        this.rideManagementClient = rideManagementClient;
    }

    public void assertCallerOwnsRide(String rideId) {
        if (!rideManagementClient.isEnabled()) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required for this action");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }

        String callerId = authentication.getName();
        RideSummaryResponse ride = rideManagementClient.getRide(rideId);

        if (ride.passengerId() == null || !ride.passengerId().equals(callerId)) {
            throw new AccessDeniedException("You are not the passenger for ride " + rideId);
        }
    }
}
