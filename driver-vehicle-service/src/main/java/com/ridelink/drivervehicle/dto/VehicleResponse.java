package com.ridelink.drivervehicle.dto;

import java.time.Instant;
import com.ridelink.drivervehicle.model.VehicleType;

public record VehicleResponse(
    String id,
    String driverId,
    String registrationNumber,
    String make,
    String model,
    Integer manufactureYear,
    String color,
    VehicleType vehicleType,
    Integer seatCapacity,
    Instant createdAt,
    Instant updatedAt
) {}
