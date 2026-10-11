package com.ridelink.ride_management_service.repository;

import com.ridelink.ride_management_service.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByRiderAccountId(Long riderAccountId);

    List<Ride> findByDriverId(Long driverId);

    List<Ride> findByStatus(String status);
}