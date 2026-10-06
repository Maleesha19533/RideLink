package com.ridelink.drivervehicle_service.repository;

import com.ridelink.drivervehicle_service.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    Optional<Vehicle> findByDriverId(Long driverId);

    boolean existsByRegistrationNumber(String registrationNumber);
}