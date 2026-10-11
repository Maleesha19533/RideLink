package com.ridelink.farepayment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ProcessPaymentRequest {

    @NotBlank(message = "Ride ID is required")
    private String rideId;

    @NotNull(message = "Fare ID is required")
    private Long fareId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    public ProcessPaymentRequest() {
    }

    public ProcessPaymentRequest(String rideId, Long fareId, BigDecimal amount, String paymentMethod) {
        this.rideId = rideId;
        this.fareId = fareId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public Long getFareId() {
        return fareId;
    }

    public void setFareId(Long fareId) {
        this.fareId = fareId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
