package com.ridelink.farepayment.service;

import com.ridelink.farepayment.client.RideManagementClient;
import com.ridelink.farepayment.client.dto.RideSummaryResponse;
import com.ridelink.farepayment.exception.ApiException;
import com.ridelink.farepayment.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * The plain FareServiceTest/PaymentServiceTest suite runs with
 * ridelink.services.ride-management.enabled=false (see src/test/resources/application.properties)
 * so it can run without a live Ride Management Service. This test class turns the
 * integration back on and mocks RideManagementClient to prove the validation logic itself
 * is correct, independent of whether the real Ride Management Service is up.
 */
@SpringBootTest
@TestPropertySource(properties = "ridelink.services.ride-management.enabled=true")
class FareServiceRideIntegrationTest {

    @Autowired
    private FareService fareService;

    @MockBean
    private RideManagementClient rideManagementClient;

    @Test
    void calculateFinalFare_succeedsWhenRideIsCompleted() {
        when(rideManagementClient.isEnabled()).thenReturn(true);
        when(rideManagementClient.getRide("RIDE-COMPLETED")).thenReturn(
                new RideSummaryResponse("RIDE-COMPLETED", "passenger-1", "driver-1", "COMPLETED", new BigDecimal("10")));

        var response = fareService.calculateFinalFare("RIDE-COMPLETED", new BigDecimal("10"));

        assertEquals("FINAL", response.getStatus());
        assertEquals(new BigDecimal("900.00"), response.getFareAmount());
    }

    @Test
    void calculateFinalFare_rejectsRideThatIsNotYetCompleted() {
        when(rideManagementClient.isEnabled()).thenReturn(true);
        when(rideManagementClient.getRide("RIDE-IN-PROGRESS")).thenReturn(
                new RideSummaryResponse("RIDE-IN-PROGRESS", "passenger-1", "driver-1", "IN_PROGRESS", new BigDecimal("10")));

        ApiException exception = assertThrows(ApiException.class,
                () -> fareService.calculateFinalFare("RIDE-IN-PROGRESS", new BigDecimal("10")));

        assertTrue(exception.getMessage().contains("current status: IN_PROGRESS"));
    }

    @Test
    void calculateFinalFare_propagatesNotFoundWhenRideManagementDoesNotKnowTheRide() {
        when(rideManagementClient.isEnabled()).thenReturn(true);
        when(rideManagementClient.getRide("RIDE-UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Ride not found in Ride Management Service: RIDE-UNKNOWN"));

        assertThrows(ResourceNotFoundException.class,
                () -> fareService.calculateFinalFare("RIDE-UNKNOWN", new BigDecimal("10")));
    }

    @Test
    void calculateFinalFare_usesTheRidesRecordedDistanceNotTheRequestedOne() {
        when(rideManagementClient.isEnabled()).thenReturn(true);
        when(rideManagementClient.getRide("RIDE-DIST-1")).thenReturn(
                new RideSummaryResponse("RIDE-DIST-1", "passenger-1", "driver-1", "COMPLETED", new BigDecimal("10")));

        // A passenger claims the ride was only 0.1 km - the fare must still be based on 10 km.
        var response = fareService.calculateFinalFare("RIDE-DIST-1", new BigDecimal("0.1"));

        assertEquals(0, new BigDecimal("900").compareTo(response.getFareAmount()));
        assertEquals(0, new BigDecimal("10").compareTo(response.getDistanceKm()));
    }

    @Test
    void calculateFinalFare_rejectsARideWithNoRecordedDistance() {
        when(rideManagementClient.isEnabled()).thenReturn(true);
        when(rideManagementClient.getRide("RIDE-DIST-2")).thenReturn(
                new RideSummaryResponse("RIDE-DIST-2", "passenger-1", "driver-1", "COMPLETED", null));

        ApiException exception = assertThrows(ApiException.class,
                () -> fareService.calculateFinalFare("RIDE-DIST-2", new BigDecimal("10")));

        assertTrue(exception.getMessage().contains("no distance recorded"));
    }

    @Test
    void calculateFinalFare_calledTwiceReturnsTheSameFinalFareInsteadOfCreatingAnother() {
        when(rideManagementClient.isEnabled()).thenReturn(true);
        when(rideManagementClient.getRide("RIDE-DIST-3")).thenReturn(
                new RideSummaryResponse("RIDE-DIST-3", "passenger-1", "driver-1", "COMPLETED", new BigDecimal("10")));

        var first = fareService.calculateFinalFare("RIDE-DIST-3", new BigDecimal("10"));
        var second = fareService.calculateFinalFare("RIDE-DIST-3", new BigDecimal("1"));

        assertEquals(first.getFareId(), second.getFareId());
        assertEquals(0, new BigDecimal("900").compareTo(second.getFareAmount()));
    }
}
