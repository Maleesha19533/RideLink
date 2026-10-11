package com.ridelink.farepayment.dto.response;

import java.math.BigDecimal;

public class PaymentResponse {
    private String paymentId;
    private String rideId;
    private BigDecimal amount;
    private String status;
    private String message;
    private String failureReason;

    public PaymentResponse() {
    }

    public PaymentResponse(String paymentId, String rideId, BigDecimal amount, String status,
                          String message, String failureReason) {
        this.paymentId = paymentId;
        this.rideId = rideId;
        this.amount = amount;
        this.status = status;
        this.message = message;
        this.failureReason = failureReason;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
