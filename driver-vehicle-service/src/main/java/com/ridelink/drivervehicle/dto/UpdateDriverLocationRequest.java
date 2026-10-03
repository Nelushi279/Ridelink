package com.ridelink.drivervehicle.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateDriverLocationRequest(
    @NotNull @DecimalMin("-90") @DecimalMax("90")
    @Schema(description = "Latitude between -90 and 90, inclusive", example = "6.9271") Double latitude,
    @NotNull @DecimalMin("-180") @DecimalMax("180")
    @Schema(description = "Longitude between -180 and 180, inclusive", example = "79.8612") Double longitude
) {}