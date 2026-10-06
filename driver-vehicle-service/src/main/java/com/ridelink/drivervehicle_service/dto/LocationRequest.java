package com.ridelink.drivervehicle_service.dto;

import jakarta.validation.constraints.NotBlank;

public class LocationRequest {

    @NotBlank(message = "Current location is required")
    private String currentLocation;

    public LocationRequest() {
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }
}