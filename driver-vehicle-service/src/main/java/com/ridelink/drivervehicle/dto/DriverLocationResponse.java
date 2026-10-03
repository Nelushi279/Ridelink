package com.ridelink.drivervehicle.dto;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

public record DriverLocationResponse(
    @Schema(example = "driver-123") String driverId,
    @Schema(example = "6.9271") Double latitude,
    @Schema(example = "79.8612") Double longitude,
    Instant locationUpdatedAt
) {}