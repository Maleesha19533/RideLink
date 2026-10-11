package com.ridelink.farepayment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class EstimateFareRequest {

    @NotBlank(message = "Ride ID is required")
    private String rideId;

    @NotNull(message = "Distance is required")
    @PositiveOrZero(message = "Distance must be a valid non-negative number")
    private BigDecimal distanceKm;

    public EstimateFareRequest() {
    }

    public EstimateFareRequest(String rideId, BigDecimal distanceKm) {
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
