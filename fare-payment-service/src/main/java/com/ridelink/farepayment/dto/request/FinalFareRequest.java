package com.ridelink.farepayment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class FinalFareRequest {

    @NotBlank(message = "Ride ID is required")
    private String rideId;

    /**
     * Optional. When the Ride Management integration is on, the final fare is calculated from
     * the distance Ride Management recorded for the ride and this value is ignored. It is only
     * used when the integration is switched off (isolated testing).
     */
    @PositiveOrZero(message = "Distance must be a valid non-negative number")
    private BigDecimal distanceKm;

    public FinalFareRequest() {
    }

    public FinalFareRequest(String rideId, BigDecimal distanceKm) {
        this.rideId = rideId;
        this.distanceKm = distanceKm;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }
}
