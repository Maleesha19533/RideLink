package com.ridelink.farepayment.dto.response;

import com.ridelink.farepayment.entity.PaymentRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentStatusResponse {
    private String paymentId;
    private String rideId;
    private Long fareId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentStatusResponse() {
    }

    public PaymentStatusResponse(String paymentId, String rideId, Long fareId, BigDecimal amount,
                                  String paymentMethod, String status, String failureReason,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.paymentId = paymentId;
        this.rideId = rideId;
        this.fareId = fareId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentStatusResponse from(PaymentRecord record) {
        return new PaymentStatusResponse(
                record.getPaymentId(),
                record.getRideId(),
                record.getFareId(),
                record.getAmount(),
                record.getPaymentMethod(),
                record.getStatus(),
                record.getFailureReason(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
