package com.ridelink.farepayment.client.dto;

import java.math.BigDecimal;

/**
 * Expected shape of GET {ride-management-base-url}/api/internal/rides/{rideId}
 * (authenticated with the X-Internal-Api-Key header).
 *
 * This is an ASSUMED contract, not a confirmed one - the Ride Management Service is owned by
 * another member of this group. Field names/paths here must be checked against (and, if
 * needed, adjusted to match) whatever that service actually returns before the integrated
 * demo. Document the agreed final contract in the group's technical report.
 */
public record RideSummaryResponse(
        String rideId,
        String passengerId,
        String driverId,
        String status,
        BigDecimal distanceKm
) {
    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }
}
