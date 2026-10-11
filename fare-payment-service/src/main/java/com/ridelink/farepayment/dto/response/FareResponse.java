package com.ridelink.farepayment.dto.response;

import java.math.BigDecimal;

public class FareResponse {
    private Long fareId;
    private String rideId;
    private BigDecimal distanceKm;
    private BigDecimal baseFare;
    private BigDecimal perKmRate;
    private BigDecimal fareAmount;
    private String status;

    public FareResponse() {
    }

    public FareResponse(Long fareId, String rideId, BigDecimal distanceKm,
                        BigDecimal baseFare, BigDecimal perKmRate,
                        BigDecimal fareAmount, String status) {
        this.fareId = fareId;
        this.rideId = rideId;
        this.distanceKm = distanceKm;
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.fareAmount = fareAmount;
        this.status = status;
    }

    public Long getFareId() {
        return fareId;
    }

    public void setFareId(Long fareId) {
        this.fareId = fareId;
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

    public BigDecimal getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(BigDecimal fareAmount) {
        this.fareAmount = fareAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}