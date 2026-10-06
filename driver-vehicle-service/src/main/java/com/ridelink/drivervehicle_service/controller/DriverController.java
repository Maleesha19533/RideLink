package com.ridelink.drivervehicle_service.controller;

import com.ridelink.drivervehicle_service.dto.AvailabilityRequest;
import com.ridelink.drivervehicle_service.dto.DriverRequest;
import com.ridelink.drivervehicle_service.dto.DriverResponse;
import com.ridelink.drivervehicle_service.dto.LocationRequest;
import com.ridelink.drivervehicle_service.security.AuthService;
import com.ridelink.drivervehicle_service.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;
    private final AuthService authService;

    public DriverController(
            DriverService driverService,
            AuthService authService) {

        this.driverService = driverService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @Valid @RequestBody DriverRequest request) {

        authService.validateDriver(authorizationHeader);

        DriverResponse response =
                driverService.createDriver(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getDriver(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @PathVariable Long driverId) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                driverService.getDriver(driverId)
        );
    }

    @PutMapping("/{driverId}/availability")
    public ResponseEntity<DriverResponse> updateAvailability(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @PathVariable Long driverId,
            @Valid @RequestBody AvailabilityRequest request) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                driverService.updateAvailability(driverId, request)
        );
    }

    @PutMapping("/{driverId}/location")
    public ResponseEntity<DriverResponse> updateLocation(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @PathVariable Long driverId,
            @Valid @RequestBody LocationRequest request) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                driverService.updateLocation(driverId, request)
        );
    }

    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @RequestParam(required = false) String serviceArea) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                driverService.getAvailableDrivers(serviceArea)
        );
    }
}