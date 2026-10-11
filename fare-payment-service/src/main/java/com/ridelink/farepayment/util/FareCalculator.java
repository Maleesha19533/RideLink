package com.ridelink.farepayment.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FareCalculator {

    public static final BigDecimal BASE_FARE = new BigDecimal("100");
    public static final BigDecimal PER_KM_RATE = new BigDecimal("80");

    public static BigDecimal calculateFare(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Distance must be a valid non-negative number.");
        }

        BigDecimal distanceCost = distanceKm.multiply(PER_KM_RATE);
        return BASE_FARE.add(distanceCost).setScale(2, RoundingMode.HALF_UP);
    }
}
