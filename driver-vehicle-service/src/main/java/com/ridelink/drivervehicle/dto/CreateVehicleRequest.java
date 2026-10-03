package com.ridelink.drivervehicle.dto;

import com.ridelink.drivervehicle.model.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CreateVehicleRequest(
    @NotBlank @Size(max = 128) @Schema(example = "driver-123") String driverId,
    @NotBlank @Size(max = 30) @Schema(example = "CAB-1234") String registrationNumber,
    @NotBlank @Size(max = 80) @Schema(example = "Toyota") String make,
    @NotBlank @Size(max = 80) @Schema(example = "Aqua") String model,
    @NotNull @Min(1980) @Schema(example = "2022", description = "1980 through the current year plus one") Integer manufactureYear,
    @NotBlank @Size(max = 50) @Schema(example = "White") String color,
    @NotNull @Schema(example = "CAR") VehicleType vehicleType,
    @NotNull @Min(1) @Max(16) @Schema(example = "4") Integer seatCapacity
) {}
