package com.ridelink.drivervehicle_service.service;

import com.ridelink.drivervehicle_service.dto.VehicleRequest;
import com.ridelink.drivervehicle_service.entity.Driver;
import com.ridelink.drivervehicle_service.entity.Vehicle;
import com.ridelink.drivervehicle_service.repository.DriverRepository;
import com.ridelink.drivervehicle_service.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    public Vehicle createVehicle(VehicleRequest request) {

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Driver not found with ID: " + request.getDriverId()
                        ));

        if (vehicleRepository
                .existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new RuntimeException(
                    "Vehicle registration number already exists"
            );
        }

        if (vehicleRepository.findByDriverId(request.getDriverId()).isPresent()) {
            throw new RuntimeException(
                    "This driver already has a vehicle"
            );
        }

        Vehicle vehicle = new Vehicle();

        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setColor(request.getColor());
        vehicle.setDriver(driver);

        return vehicleRepository.save(vehicle);
    }

    public Vehicle getVehicle(Long vehicleId) {

        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Vehicle not found with ID: " + vehicleId
                        ));
    }

    public Vehicle getVehicleByDriver(Long driverId) {

        return vehicleRepository.findByDriverId(driverId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Vehicle not found for driver ID: " + driverId
                        ));
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }
}