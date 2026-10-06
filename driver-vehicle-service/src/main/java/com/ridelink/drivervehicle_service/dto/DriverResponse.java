package com.ridelink.drivervehicle_service.dto;

public class DriverResponse {

    private Long id;
    private Long accountId;
    private String licenseNumber;
    private String serviceArea;
    private boolean available;
    private String currentLocation;

    public DriverResponse() {
    }

    public DriverResponse(Long id, Long accountId, String licenseNumber,
                          String serviceArea, boolean available,
                          String currentLocation) {
        this.id = id;
        this.accountId = accountId;
        this.licenseNumber = licenseNumber;
        this.serviceArea = serviceArea;
        this.available = available;
        this.currentLocation = currentLocation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }
}