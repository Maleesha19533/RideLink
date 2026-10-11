package com.ridelink.ride_management_service.dto;

import jakarta.validation.constraints.NotNull;

public class DriverAssignmentRequest {

    @NotNull(message = "Driver ID is required")
    private Long driverId;

    public DriverAssignmentRequest() {
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }
}