package com.ridelink.drivervehicle_service.exception;

public class DriverNotFoundException extends RuntimeException {

    public DriverNotFoundException(String message) {
        super(message);
    }
}