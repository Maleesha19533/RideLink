package com.ridelink.ride_management_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RideRequest {

    @NotNull(message = "Rider account ID is required")
    private Long riderAccountId;

    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    @NotBlank(message = "Destination is required")
    private String destination;

    public RideRequest() {
    }

    public Long getRiderAccountId() {
        return riderAccountId;
    }

    public void setRiderAccountId(Long riderAccountId) {
        this.riderAccountId = riderAccountId;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}