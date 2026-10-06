package com.ridelink.drivervehicle_service;

import com.ridelink.drivervehicle_service.dto.VehicleRequest;
import com.ridelink.drivervehicle_service.entity.Driver;
import com.ridelink.drivervehicle_service.entity.Vehicle;
import com.ridelink.drivervehicle_service.repository.DriverRepository;
import com.ridelink.drivervehicle_service.repository.VehicleRepository;
import com.ridelink.drivervehicle_service.service.VehicleService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VehicleServiceTest {

    private VehicleRepository vehicleRepository;
    private DriverRepository driverRepository;
    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {

        vehicleRepository =
                Mockito.mock(VehicleRepository.class);

        driverRepository =
                Mockito.mock(DriverRepository.class);

        vehicleService =
                new VehicleService(
                        vehicleRepository,
                        driverRepository
                );
    }

    @Test
    void vehicleServiceShouldBeCreated() {

        assertNotNull(vehicleService);
    }

    @Test
    void createVehicleShouldReturnCreatedVehicle() {

        Driver driver = createTestDriver();

        VehicleRequest request = new VehicleRequest();

        request.setDriverId(1L);
        request.setRegistrationNumber("CAB-1234");
        request.setVehicleType("CAR");
        request.setBrand("Toyota");
        request.setModel("Aqua");
        request.setColor("White");

        Mockito.when(
                driverRepository.findById(1L)
        ).thenReturn(Optional.of(driver));

        Mockito.when(
                vehicleRepository.existsByRegistrationNumber("CAB-1234")
        ).thenReturn(false);

        Mockito.when(
                vehicleRepository.findByDriverId(1L)
        ).thenReturn(Optional.empty());

        Mockito.when(
                vehicleRepository.save(Mockito.any(Vehicle.class))
        ).thenAnswer(invocation -> {

            Vehicle vehicle = invocation.getArgument(0);
            vehicle.setId(1L);

            return vehicle;
        });

        Vehicle result =
                vehicleService.createVehicle(request);

        assertNotNull(result);

        assertEquals(
                1L,
                result.getId()
        );

        assertEquals(
                "CAB-1234",
                result.getRegistrationNumber()
        );

        assertEquals(
                "CAR",
                result.getVehicleType()
        );

        assertEquals(
                "Toyota",
                result.getBrand()
        );

        assertEquals(
                "Aqua",
                result.getModel()
        );

        assertEquals(
                "White",
                result.getColor()
        );

        assertEquals(
                1L,
                result.getDriver().getId()
        );

        Mockito.verify(
                vehicleRepository,
                Mockito.times(1)
        ).save(Mockito.any(Vehicle.class));
    }

    @Test
    void createVehicleShouldFailWhenDriverDoesNotExist() {

        VehicleRequest request =
                createVehicleRequest();

        Mockito.when(
                driverRepository.findById(1L)
        ).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> vehicleService.createVehicle(request)
        );

        assertEquals(
                "Driver not found with ID: 1",
                exception.getMessage()
        );

        Mockito.verify(
                vehicleRepository,
                Mockito.never()
        ).save(Mockito.any(Vehicle.class));
    }

    @Test
    void createVehicleShouldFailWhenRegistrationNumberExists() {

        VehicleRequest request =
                createVehicleRequest();

        Driver driver =
                createTestDriver();

        Mockito.when(
                driverRepository.findById(1L)
        ).thenReturn(Optional.of(driver));

        Mockito.when(
                vehicleRepository.existsByRegistrationNumber("CAB-1234")
        ).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> vehicleService.createVehicle(request)
        );

        assertEquals(
                "Vehicle registration number already exists",
                exception.getMessage()
        );
    }

    @Test
    void createVehicleShouldFailWhenDriverAlreadyHasVehicle() {

        VehicleRequest request =
                createVehicleRequest();

        Driver driver =
                createTestDriver();

        Vehicle existingVehicle =
                createTestVehicle(driver);

        Mockito.when(
                driverRepository.findById(1L)
        ).thenReturn(Optional.of(driver));

        Mockito.when(
                vehicleRepository.existsByRegistrationNumber("CAB-1234")
        ).thenReturn(false);

        Mockito.when(
                vehicleRepository.findByDriverId(1L)
        ).thenReturn(Optional.of(existingVehicle));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> vehicleService.createVehicle(request)
        );

        assertEquals(
                "This driver already has a vehicle",
                exception.getMessage()
        );
    }

    @Test
    void getVehicleShouldReturnVehicle() {

        Driver driver =
                createTestDriver();

        Vehicle vehicle =
                createTestVehicle(driver);

        Mockito.when(
                vehicleRepository.findById(1L)
        ).thenReturn(Optional.of(vehicle));

        Vehicle result =
                vehicleService.getVehicle(1L);

        assertNotNull(result);

        assertEquals(
                "CAB-1234",
                result.getRegistrationNumber()
        );
    }

    @Test
    void getVehicleByDriverShouldReturnVehicle() {

        Driver driver =
                createTestDriver();

        Vehicle vehicle =
                createTestVehicle(driver);

        Mockito.when(
                vehicleRepository.findByDriverId(1L)
        ).thenReturn(Optional.of(vehicle));

        Vehicle result =
                vehicleService.getVehicleByDriver(1L);

        assertNotNull(result);

        assertEquals(
                1L,
                result.getDriver().getId()
        );
    }

    @Test
    void getAllVehiclesShouldReturnVehicleList() {

        Driver driver =
                createTestDriver();

        Vehicle vehicle =
                createTestVehicle(driver);

        Mockito.when(
                vehicleRepository.findAll()
        ).thenReturn(List.of(vehicle));

        List<Vehicle> result =
                vehicleService.getAllVehicles();

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "CAB-1234",
                result.get(0).getRegistrationNumber()
        );
    }

    private Driver createTestDriver() {

        Driver driver = new Driver();

        driver.setId(1L);
        driver.setAccountId(3L);
        driver.setLicenseNumber("B1234567");
        driver.setServiceArea("Kandy");
        driver.setCurrentLocation("Kandy City");
        driver.setAvailable(true);

        return driver;
    }

    private VehicleRequest createVehicleRequest() {

        VehicleRequest request =
                new VehicleRequest();

        request.setDriverId(1L);
        request.setRegistrationNumber("CAB-1234");
        request.setVehicleType("CAR");
        request.setBrand("Toyota");
        request.setModel("Aqua");
        request.setColor("White");

        return request;
    }

    private Vehicle createTestVehicle(
            Driver driver) {

        Vehicle vehicle = new Vehicle();

        vehicle.setId(1L);
        vehicle.setRegistrationNumber("CAB-1234");
        vehicle.setVehicleType("CAR");
        vehicle.setBrand("Toyota");
        vehicle.setModel("Aqua");
        vehicle.setColor("White");
        vehicle.setDriver(driver);

        return vehicle;
    }
}