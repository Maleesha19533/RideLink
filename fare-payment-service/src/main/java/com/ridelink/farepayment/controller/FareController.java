package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.request.EstimateFareRequest;
import com.ridelink.farepayment.dto.request.FinalFareRequest;
import com.ridelink.farepayment.dto.response.FareResponse;
import com.ridelink.farepayment.security.RideOwnershipGuard;
import com.ridelink.farepayment.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fare")
@Tag(name = "Fare API", description = "Fare estimation and final fare calculation")
public class FareController {

    private final FareService fareService;
    private final RideOwnershipGuard rideOwnershipGuard;

    public FareController(FareService fareService, RideOwnershipGuard rideOwnershipGuard) {
        this.fareService = fareService;
        this.rideOwnershipGuard = rideOwnershipGuard;
    }

    @PostMapping("/estimate")
    @Operation(summary = "Estimate fare for a ride", description = "Calculates the estimated fare for the provided distance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estimated fare calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ride ID or distance")
    })
    public ResponseEntity<FareResponse> estimateFare(@Valid @RequestBody EstimateFareRequest request) {
        FareResponse response = fareService.estimateFare(request.getRideId(), request.getDistanceKm());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/final")
    @Operation(summary = "Calculate final fare for a ride",
            description = "Calculates the final fare from the distance Ride Management recorded for the completed ride "
                    + "(the distanceKm in the request is ignored when the integration is on). Only the ride's own "
                    + "passenger or an admin may call this. A ride only ever gets one FINAL fare - repeat calls return it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Final fare calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ride ID, or ride not yet completed"),
            @ApiResponse(responseCode = "403", description = "Caller is not the ride's passenger"),
            @ApiResponse(responseCode = "404", description = "Ride not found in Ride Management Service"),
            @ApiResponse(responseCode = "503", description = "Ride Management Service unavailable")
    })
    public ResponseEntity<FareResponse> calculateFinalFare(@Valid @RequestBody FinalFareRequest request) {
        // Ownership first: only the ride's passenger (or an admin) may trigger the final fare.
        rideOwnershipGuard.assertCallerOwnsRide(request.getRideId());
        FareResponse response = fareService.calculateFinalFare(request.getRideId(), request.getDistanceKm());
        return ResponseEntity.ok(response);
    }
}
