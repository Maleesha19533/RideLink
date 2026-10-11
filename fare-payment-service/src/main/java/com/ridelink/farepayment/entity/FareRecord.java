package com.ridelink.farepayment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "fare_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_fare_records_final_ride_id", columnNames = "final_ride_id")
)
public class FareRecord {

    public static final String TYPE_ESTIMATED = "ESTIMATED";
    public static final String TYPE_FINAL = "FINAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String rideId;

    @Column(nullable = false)
    private BigDecimal distanceKm;

    @Column(nullable = false)
    private BigDecimal baseFare;

    @Column(nullable = false)
    private BigDecimal perKmRate;

    @Column(nullable = false)
    private BigDecimal estimatedFare;

    @Column(nullable = false)
    private BigDecimal finalFare;

    @Column(nullable = false)
    private String fareType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Database-level guarantee that a ride has at most ONE FINAL fare.
     *
     * A plain unique constraint on (rideId, fareType) would be wrong: a ride may legitimately have
     * many ESTIMATED rows. Instead this column holds the rideId ONLY for FINAL rows and stays NULL
     * for every other fare type. A unique constraint on it therefore allows unlimited ESTIMATED
     * rows (NULLs never collide in PostgreSQL, MySQL or H2) but rejects a second FINAL row for the
     * same ride, even when two requests race each other. Never set it by hand - it is derived from
     * rideId + fareType by {@link #syncFinalRideId()}.
     */
    @Column(name = "final_ride_id")
    private String finalRideId;

    public FareRecord() {
    }

    public FareRecord(String rideId, BigDecimal distanceKm, BigDecimal baseFare, BigDecimal perKmRate,
                     BigDecimal estimatedFare, BigDecimal finalFare, String fareType, LocalDateTime createdAt) {
        this.rideId = rideId;
        this.distanceKm = distanceKm;
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.estimatedFare = estimatedFare;
        this.finalFare = finalFare;
        this.fareType = fareType;
        this.createdAt = createdAt;
    }

    @PrePersist
    @PreUpdate
    void syncFinalRideId() {
        this.finalRideId = TYPE_FINAL.equals(fareType) ? rideId : null;
    }

    public Long getId() {
        return id;
    }

    public String getFinalRideId() {
        return finalRideId;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(BigDecimal baseFare) {
        this.baseFare = baseFare;
    }

    public BigDecimal getPerKmRate() {
        return perKmRate;
    }

    public void setPerKmRate(BigDecimal perKmRate) {
        this.perKmRate = perKmRate;
    }

    public BigDecimal getEstimatedFare() {
        return estimatedFare;
    }

    public void setEstimatedFare(BigDecimal estimatedFare) {
        this.estimatedFare = estimatedFare;
    }

    public BigDecimal getFinalFare() {
        return finalFare;
    }

    public void setFinalFare(BigDecimal finalFare) {
        this.finalFare = finalFare;
    }

    public String getFareType() {
        return fareType;
    }

    public void setFareType(String fareType) {
        this.fareType = fareType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
