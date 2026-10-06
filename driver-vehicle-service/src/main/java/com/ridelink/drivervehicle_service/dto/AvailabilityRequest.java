package com.ridelink.drivervehicle_service.dto;

import jakarta.validation.constraints.NotNull;

public class AvailabilityRequest {

    @NotNull(message = "Availability status is required")
    private Boolean available;

    public AvailabilityRequest() {
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}