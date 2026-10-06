package com.ridelink.drivervehicle_service.service;

import com.ridelink.drivervehicle_service.dto.AvailabilityRequest;
import com.ridelink.drivervehicle_service.dto.DriverRequest;
import com.ridelink.drivervehicle_service.dto.DriverResponse;
import com.ridelink.drivervehicle_service.dto.LocationRequest;
import com.ridelink.drivervehicle_service.entity.Driver;
import com.ridelink.drivervehicle_service.repository.DriverRepository;
import org.springframework.stereotype.Service;
import com.ridelink.drivervehicle_service.exception.DriverNotFoundException;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public DriverResponse createDriver(DriverRequest request) {

        if (driverRepository.findByAccountId(request.getAccountId()).isPresent()) {
            throw new RuntimeException("Driver profile already exists for this account");
        }

        if (driverRepository.findByLicenseNumber(request.getLicenseNumber()).isPresent()) {
            throw new RuntimeException("License number already exists");
        }

        Driver driver = new Driver();

        driver.setAccountId(request.getAccountId());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setServiceArea(request.getServiceArea());
        driver.setCurrentLocation(request.getCurrentLocation());
        driver.setAvailable(false);

        Driver savedDriver = driverRepository.save(driver);

        return convertToResponse(savedDriver);
    }

    public DriverResponse getDriver(Long driverId) {

        Driver driver = findDriver(driverId);

        return convertToResponse(driver);
    }

    public DriverResponse updateAvailability(
            Long driverId,
            AvailabilityRequest request) {

        Driver driver = findDriver(driverId);

        driver.setAvailable(request.getAvailable());

        Driver updatedDriver = driverRepository.save(driver);

        return convertToResponse(updatedDriver);
    }

    public DriverResponse updateLocation(
            Long driverId,
            LocationRequest request) {

        Driver driver = findDriver(driverId);

        driver.setCurrentLocation(request.getCurrentLocation());

        Driver updatedDriver = driverRepository.save(driver);

        return convertToResponse(updatedDriver);
    }

    public List<DriverResponse> getAvailableDrivers(String serviceArea) {

        List<Driver> drivers;

        if (serviceArea == null || serviceArea.isBlank()) {
            drivers = driverRepository.findByAvailableTrue();
        } else {
            drivers =
                    driverRepository
                            .findByAvailableTrueAndServiceAreaIgnoreCase(serviceArea);
        }

        return drivers.stream()
                .map(this::convertToResponse)
                .toList();
    }

    private Driver findDriver(Long driverId) {

        return driverRepository.findById(driverId)
                .orElseThrow(() ->
                        new DriverNotFoundException(
                                "Driver not found with ID: " + driverId
                        ));
    }

    private DriverResponse convertToResponse(Driver driver) {

        return new DriverResponse(
                driver.getId(),
                driver.getAccountId(),
                driver.getLicenseNumber(),
                driver.getServiceArea(),
                driver.isAvailable(),
                driver.getCurrentLocation()
        );
    }
}