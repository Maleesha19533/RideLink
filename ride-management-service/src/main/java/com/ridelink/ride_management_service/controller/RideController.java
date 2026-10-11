
package com.ridelink.ride_management_service.controller;

import com.ridelink.ride_management_service.client.DriverClient;
import com.ridelink.ride_management_service.dto.DriverAssignmentRequest;
import com.ridelink.ride_management_service.dto.RideRequest;
import com.ridelink.ride_management_service.dto.RideResponse;
import com.ridelink.ride_management_service.security.AuthService;
import com.ridelink.ride_management_service.service.RideService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;
    private final AuthService authService;
    private final DriverClient driverClient;

    public RideController(
            RideService rideService,
            AuthService authService,
            DriverClient driverClient) {

        this.rideService = rideService;
        this.authService = authService;
        this.driverClient = driverClient;
    }

    // 1. Create Ride Request
    @PostMapping
    public ResponseEntity<RideResponse> createRide(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @Valid @RequestBody RideRequest request) {

        authService.validateRider(authorizationHeader);

        RideResponse response =
                rideService.createRide(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 2. Get Ride by ID
    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getRide(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @PathVariable Long rideId) {

        authService.validateAuthenticatedUser(
                authorizationHeader
        );

        return ResponseEntity.ok(
                rideService.getRide(rideId)
        );
    }

    // 3. Get Rides by Rider
    @GetMapping("/rider/{riderAccountId}")
    public ResponseEntity<List<RideResponse>> getRidesByRider(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @PathVariable Long riderAccountId) {

        authService.validateRider(authorizationHeader);

        return ResponseEntity.ok(
                rideService.getRidesByRider(riderAccountId)
        );
    }

    // 4. Get Rides by Driver
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<RideResponse>> getRidesByDriver(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @PathVariable Long driverId) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                rideService.getRidesByDriver(driverId)
        );
    }

    // 5. Assign Driver - Microservice Integration
    @PutMapping("/{rideId}/assign-driver")
    public ResponseEntity<RideResponse> assignDriver(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @PathVariable Long rideId,
            @Valid @RequestBody DriverAssignmentRequest request) {

        // Validate JWT token
        authService.validateAuthenticatedUser(
                authorizationHeader
        );

        // Check driver availability from Driver Service (8082)
        boolean available = driverClient.isDriverAvailable(
                request.getDriverId(),
                authorizationHeader
        );

        if (!available) {
            throw new IllegalStateException(
                    "Driver is not available or does not exist"
            );
        }

        // Assign driver only when available
        RideResponse response =
                rideService.assignDriver(rideId, request);

        return ResponseEntity.ok(response);
    }

    // 6. Accept Ride
    @PutMapping("/{rideId}/accept")
    public ResponseEntity<RideResponse> acceptRide(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @PathVariable Long rideId) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                rideService.acceptRide(rideId)
        );
    }

    // 7. Start Ride
    @PutMapping("/{rideId}/start")
    public ResponseEntity<RideResponse> startRide(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @PathVariable Long rideId) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                rideService.startRide(rideId)
        );
    }

    // 8. Complete Ride
    @PutMapping("/{rideId}/complete")
    public ResponseEntity<RideResponse> completeRide(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @PathVariable Long rideId) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                rideService.completeRide(rideId)
        );
    }

    // 9. Cancel Ride
    @PutMapping("/{rideId}/cancel")
    public ResponseEntity<RideResponse> cancelRide(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @PathVariable Long rideId) {

        authService.validateAuthenticatedUser(
                authorizationHeader
        );

        return ResponseEntity.ok(
                rideService.cancelRide(rideId)
        );
    }
}
