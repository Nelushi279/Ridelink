package com.ridelink.drivervehicle.service;

import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.exception.*;
import com.ridelink.drivervehicle.model.Vehicle;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;

@Service
public class VehicleService {
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;

    public VehicleService(VehicleRepository vehicles, DriverRepository drivers) {
        this.vehicles = vehicles;
        this.drivers = drivers;
    }

    public VehicleResponse create(CreateVehicleRequest request) {
        validateYear(request.manufactureYear());
        String driverId = request.driverId().trim();
        requireDriver(driverId);
        String registration = normalizeRegistration(request.registrationNumber());
        requireUniqueRegistration(registration);

        Vehicle vehicle = new Vehicle();
        vehicle.setDriverId(driverId);
        vehicle.setRegistrationNumber(registration);
        vehicle.setMake(request.make().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setManufactureYear(request.manufactureYear());
        vehicle.setColor(request.color().trim());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setSeatCapacity(request.seatCapacity());
        Instant now = Instant.now();
        vehicle.setCreatedAt(now);
        vehicle.setUpdatedAt(now);
        return response(save(vehicle));
    }

    public VehicleResponse getById(String vehicleId) {
        return response(find(vehicleId));
    }

    public List<VehicleResponse> getByDriverId(String driverId) {
        requireDriver(driverId);
        return vehicles.findByDriverId(driverId).stream().map(this::response).toList();
    }

    public VehicleResponse update(String vehicleId, UpdateVehicleRequest request) {
        if (request.isEmpty()) {
            throw new InvalidVehicleException("At least one vehicle field is required");
        }
        Vehicle vehicle = find(vehicleId);
        if (request.manufactureYear() != null) {
            validateYear(request.manufactureYear());
        }
        String registration = request.registrationNumber() == null
            ? null : normalizeRegistration(request.registrationNumber());
        if (registration != null && !registration.equals(vehicle.getRegistrationNumber())) {
            requireUniqueRegistration(registration);
        }

        if (registration != null) vehicle.setRegistrationNumber(registration);
        if (request.make() != null) vehicle.setMake(request.make().trim());
        if (request.model() != null) vehicle.setModel(request.model().trim());
        if (request.manufactureYear() != null) vehicle.setManufactureYear(request.manufactureYear());
        if (request.color() != null) vehicle.setColor(request.color().trim());
        if (request.vehicleType() != null) vehicle.setVehicleType(request.vehicleType());
        if (request.seatCapacity() != null) vehicle.setSeatCapacity(request.seatCapacity());
        vehicle.setUpdatedAt(Instant.now());
        return response(save(vehicle));
    }

    private Vehicle find(String vehicleId) {
        return vehicles.findById(vehicleId)
            .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found"));
    }

    private void requireDriver(String driverId) {
        if (!drivers.existsById(driverId)) {
            throw new DriverNotFoundException("Driver not found");
        }
    }

    private String normalizeRegistration(String registration) {
        return registration.trim().toUpperCase(Locale.ROOT);
    }

    private void requireUniqueRegistration(String registration) {
        if (vehicles.existsByRegistrationNumber(registration)) {
            throw new DuplicateVehicleException("Vehicle registration number is already in use");
        }
    }

    private void validateYear(Integer year) {
        int latestYear = Year.now(ZoneId.of("Asia/Colombo")).getValue() + 1;
        if (year == null || year < 1980 || year > latestYear) {
            throw new InvalidVehicleException("Manufacture year must be between 1980 and " + latestYear);
        }
    }

    private Vehicle save(Vehicle vehicle) {
        try {
            return vehicles.save(vehicle);
        } catch (DuplicateKeyException exception) {
            // The unique index also rejects concurrent writes that pass the earlier check.
            throw new DuplicateVehicleException("Vehicle registration number is already in use");
        }
    }

    private VehicleResponse response(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getDriverId(), vehicle.getRegistrationNumber(),
            vehicle.getMake(), vehicle.getModel(), vehicle.getManufactureYear(), vehicle.getColor(),
            vehicle.getVehicleType(), vehicle.getSeatCapacity(), vehicle.getCreatedAt(), vehicle.getUpdatedAt());
    }
}
