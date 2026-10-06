package com.ridelink.drivervehicle_service.controller;

import com.ridelink.drivervehicle_service.dto.VehicleRequest;
import com.ridelink.drivervehicle_service.entity.Vehicle;
import com.ridelink.drivervehicle_service.security.AuthService;
import com.ridelink.drivervehicle_service.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final AuthService authService;

    public VehicleController(
            VehicleService vehicleService,
            AuthService authService) {

        this.vehicleService = vehicleService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<Vehicle> createVehicle(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @Valid @RequestBody VehicleRequest request) {

        authService.validateDriver(authorizationHeader);

        Vehicle vehicle =
                vehicleService.createVehicle(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(vehicle);
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<Vehicle> getVehicle(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @PathVariable Long vehicleId) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                vehicleService.getVehicle(vehicleId)
        );
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<Vehicle> getVehicleByDriver(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @PathVariable Long driverId) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                vehicleService.getVehicleByDriver(driverId)
        );
    }

    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader) {

        authService.validateDriver(authorizationHeader);

        return ResponseEntity.ok(
                vehicleService.getAllVehicles()
        );
    }
}