package com.ridelink.farepayment.dto.request;

import jakarta.validation.constraints.NotBlank;

public class GenerateReceiptRequest {

    @NotBlank(message = "Ride ID is required")
    private String rideId;

    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    public GenerateReceiptRequest() {
    }

    public GenerateReceiptRequest(String rideId, String paymentId) {
        this.rideId = rideId;
        this.paymentId = paymentId;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
}
