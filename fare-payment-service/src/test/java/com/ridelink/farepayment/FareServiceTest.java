package com.ridelink.farepayment;

import com.ridelink.farepayment.dto.response.FareResponse;
import com.ridelink.farepayment.exception.ApiException;
import com.ridelink.farepayment.service.FareService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FareServiceTest {

    @Autowired
    private FareService fareService;

    @Test
    void estimateFare_shouldReturnCalculatedFareForValidDistance() {
        FareResponse response = fareService.estimateFare("RIDE-001", new BigDecimal("10"));

        assertEquals("RIDE-001", response.getRideId());
        assertEquals(new BigDecimal("900.00"), response.getFareAmount());
        assertEquals("ESTIMATED", response.getStatus());
    }

    @Test
    void calculateFinalFare_shouldReturnCalculatedFareForValidDistance() {
        FareResponse response = fareService.calculateFinalFare("RIDE-002", new BigDecimal("5"));

        assertEquals(new BigDecimal("500.00"), response.getFareAmount());
        assertEquals("FINAL", response.getStatus());
    }

    @Test
    void estimateFare_shouldThrowForNegativeDistance() {
        ApiException exception = assertThrows(ApiException.class,
                () -> fareService.estimateFare("RIDE-003", new BigDecimal("-1")));

        assertTrue(exception.getMessage().contains("Distance must be a valid non-negative number"));
    }

    @Test
    void estimateFare_shouldThrowForMissingRideId() {
        ApiException exception = assertThrows(ApiException.class,
                () -> fareService.estimateFare("   ", new BigDecimal("5")));

        assertTrue(exception.getMessage().contains("Ride ID is required"));
    }
}
