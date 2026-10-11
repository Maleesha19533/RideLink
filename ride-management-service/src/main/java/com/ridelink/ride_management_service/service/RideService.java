package com.ridelink.ride_management_service.service;

import com.ridelink.ride_management_service.dto.DriverAssignmentRequest;
import com.ridelink.ride_management_service.dto.RideRequest;
import com.ridelink.ride_management_service.dto.RideResponse;
import com.ridelink.ride_management_service.entity.Ride;
import com.ridelink.ride_management_service.exception.RideNotFoundException;
import com.ridelink.ride_management_service.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RideService {

    private final RideRepository rideRepository;

    public RideService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    public RideResponse createRide(RideRequest request) {

        Ride ride = new Ride();

        ride.setRiderAccountId(request.getRiderAccountId());
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDestination(request.getDestination());
        ride.setStatus("REQUESTED");
        ride.setRequestedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        return convertToResponse(savedRide);
    }

    public RideResponse getRide(Long rideId) {

        Ride ride = findRide(rideId);

        return convertToResponse(ride);
    }

    public List<RideResponse> getRidesByRider(Long riderAccountId) {

        return rideRepository
                .findByRiderAccountId(riderAccountId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<RideResponse> getRidesByDriver(Long driverId) {

        return rideRepository
                .findByDriverId(driverId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public RideResponse assignDriver(
            Long rideId,
            DriverAssignmentRequest request) {

        Ride ride = findRide(rideId);

        if (!"REQUESTED".equals(ride.getStatus())) {
            throw new RuntimeException(
                    "Driver can only be assigned to a requested ride"
            );
        }

        ride.setDriverId(request.getDriverId());
        ride.setStatus("ASSIGNED");

        Ride savedRide = rideRepository.save(ride);

        return convertToResponse(savedRide);
    }

    public RideResponse acceptRide(Long rideId) {

        Ride ride = findRide(rideId);

        if (!"ASSIGNED".equals(ride.getStatus())) {
            throw new RuntimeException(
                    "Only an assigned ride can be accepted"
            );
        }

        ride.setStatus("ACCEPTED");
        ride.setAcceptedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        return convertToResponse(savedRide);
    }

    public RideResponse startRide(Long rideId) {

        Ride ride = findRide(rideId);

        if (!"ACCEPTED".equals(ride.getStatus())) {
            throw new RuntimeException(
                    "Only an accepted ride can be started"
            );
        }

        ride.setStatus("STARTED");
        ride.setStartedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        return convertToResponse(savedRide);
    }

    public RideResponse completeRide(Long rideId) {

        Ride ride = findRide(rideId);

        if (!"STARTED".equals(ride.getStatus())) {
            throw new RuntimeException(
                    "Only a started ride can be completed"
            );
        }

        ride.setStatus("COMPLETED");
        ride.setCompletedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        return convertToResponse(savedRide);
    }

    public RideResponse cancelRide(Long rideId) {

        Ride ride = findRide(rideId);

        if ("COMPLETED".equals(ride.getStatus())) {
            throw new RuntimeException(
                    "Completed ride cannot be cancelled"
            );
        }

        if ("CANCELLED".equals(ride.getStatus())) {
            throw new RuntimeException(
                    "Ride is already cancelled"
            );
        }

        ride.setStatus("CANCELLED");
        ride.setCancelledAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        return convertToResponse(savedRide);
    }

    private Ride findRide(Long rideId) {

        return rideRepository
                .findById(rideId)
                .orElseThrow(
                        () -> new RideNotFoundException(
                                "Ride not found with ID: " + rideId
                        )
                );
    }

    private RideResponse convertToResponse(Ride ride) {

        return new RideResponse(
                ride.getId(),
                ride.getRiderAccountId(),
                ride.getDriverId(),
                ride.getPickupLocation(),
                ride.getDestination(),
                ride.getStatus(),
                ride.getRequestedAt(),
                ride.getAcceptedAt(),
                ride.getStartedAt(),
                ride.getCompletedAt(),
                ride.getCancelledAt()
        );
    }
}