package com.ridelink.farepayment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReceiptResponse {
    private String receiptId;
    private String rideId;
    private String paymentId;
    private BigDecimal amount;
    private BigDecimal finalFare;
    private LocalDateTime issuedAt;
    private String status;

    public ReceiptResponse() {
    }

    public ReceiptResponse(String receiptId, String rideId, String paymentId, BigDecimal amount,
                          BigDecimal finalFare, LocalDateTime issuedAt, String status) {
        this.receiptId = receiptId;
        this.rideId = rideId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.finalFare = finalFare;
        this.issuedAt = issuedAt;
        this.status = status;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFinalFare() {
        return finalFare;
    }

    public void setFinalFare(BigDecimal finalFare) {
        this.finalFare = finalFare;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
