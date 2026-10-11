package com.ridelink.ride_management_service;

import com.ridelink.ride_management_service.dto.DriverAssignmentRequest;
import com.ridelink.ride_management_service.dto.RideRequest;
import com.ridelink.ride_management_service.dto.RideResponse;
import com.ridelink.ride_management_service.entity.Ride;
import com.ridelink.ride_management_service.exception.RideNotFoundException;
import com.ridelink.ride_management_service.repository.RideRepository;
import com.ridelink.ride_management_service.service.RideService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private RideService rideService;

    private Ride ride;

    @BeforeEach
    void setUp() {
        ride = new Ride();
        ride.setId(1L);
        ride.setRiderAccountId(1L);
        ride.setPickupLocation("Kandy");
        ride.setDestination("Peradeniya");
        ride.setStatus("REQUESTED");
    }

    @Test
    void createRide_shouldReturnRequestedRide() {
        RideRequest request = new RideRequest();
        request.setRiderAccountId(1L);
        request.setPickupLocation("Kandy");
        request.setDestination("Peradeniya");

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> {
                    Ride savedRide = invocation.getArgument(0);
                    savedRide.setId(1L);
                    return savedRide;
                });

        RideResponse response = rideService.createRide(request);

        assertEquals(1L, response.getId());
        assertEquals("REQUESTED", response.getStatus());
        assertEquals("Kandy", response.getPickupLocation());

        verify(rideRepository).save(any(Ride.class));
    }

    @Test
    void getRide_shouldReturnRide() {
        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        RideResponse response = rideService.getRide(1L);

        assertEquals(1L, response.getId());
        assertEquals("REQUESTED", response.getStatus());
    }

    @Test
    void getRide_shouldThrowExceptionWhenNotFound() {
        when(rideRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RideNotFoundException.class,
                () -> rideService.getRide(99L)
        );
    }

    @Test
    void assignDriver_shouldChangeStatusToAssigned() {
        DriverAssignmentRequest request =
                new DriverAssignmentRequest();
        request.setDriverId(5L);

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response =
                rideService.assignDriver(1L, request);

        assertEquals("ASSIGNED", response.getStatus());
        assertEquals(5L, response.getDriverId());
    }

    @Test
    void acceptRide_shouldChangeStatusToAccepted() {
        ride.setStatus("ASSIGNED");

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response = rideService.acceptRide(1L);

        assertEquals("ACCEPTED", response.getStatus());
        assertNotNull(response.getAcceptedAt());
    }

    @Test
    void startRide_shouldChangeStatusToStarted() {
        ride.setStatus("ACCEPTED");

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response = rideService.startRide(1L);

        assertEquals("STARTED", response.getStatus());
        assertNotNull(response.getStartedAt());
    }

    @Test
    void completeRide_shouldChangeStatusToCompleted() {
        ride.setStatus("STARTED");

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response = rideService.completeRide(1L);

        assertEquals("COMPLETED", response.getStatus());
        assertNotNull(response.getCompletedAt());
    }

    @Test
    void cancelRide_shouldChangeStatusToCancelled() {
        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response = rideService.cancelRide(1L);

        assertEquals("CANCELLED", response.getStatus());
        assertNotNull(response.getCancelledAt());
    }

    @Test
    void cancelRide_shouldFailWhenAlreadyCompleted() {
        ride.setStatus("COMPLETED");

        when(rideRepository.findById(1L))
                .thenReturn(Optional.of(ride));

        assertThrows(
                RuntimeException.class,
                () -> rideService.cancelRide(1L)
        );

        verify(rideRepository, never()).save(any(Ride.class));
    }
}
