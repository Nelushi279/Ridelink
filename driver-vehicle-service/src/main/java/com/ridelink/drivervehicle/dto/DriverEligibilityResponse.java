package com.ridelink.drivervehicle.dto;

import com.ridelink.drivervehicle.model.DriverStatus;
import com.ridelink.drivervehicle.model.DriverAvailability;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Read-only driver eligibility for future Ride Management validation")
public record DriverEligibilityResponse(
    @Schema(example = "driver-123") String driverId,
    @Schema(example = "ACTIVE") DriverStatus status,
    @Schema(example = "AVAILABLE") DriverAvailability availability,
    @Schema(description = "Whether the driver has at least one registered vehicle", example = "true") boolean hasRegisteredVehicle,
    @Schema(description = "True only for ACTIVE + AVAILABLE + at least one registered vehicle", example = "true") boolean eligible
) {}