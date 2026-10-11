package com.ridelink.farepayment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipt_records")
public class ReceiptRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String receiptId;

    // Unique: a ride can only ever have one receipt. The service checks this first for a friendly
    // message, but this constraint is what actually stops two simultaneous requests both winning.
    @Column(nullable = false, unique = true)
    private String rideId;

    @Column(nullable = false)
    private String paymentId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal finalFare;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private String status;

    public ReceiptRecord() {
    }

    public ReceiptRecord(String receiptId, String rideId, String paymentId, BigDecimal amount,
                        BigDecimal finalFare, LocalDateTime issuedAt, String status) {
        this.receiptId = receiptId;
        this.rideId = rideId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.finalFare = finalFare;
        this.issuedAt = issuedAt;
        this.status = status;
    }

    public Long getId() {
        return id;
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
