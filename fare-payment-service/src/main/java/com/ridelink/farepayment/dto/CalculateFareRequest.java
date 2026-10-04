package com.ridelink.farepayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CalculateFareRequest(
        @NotBlank(message = "must not be blank") String rideId,
        @NotBlank(message = "must not be blank") String passengerAccountId,
        @NotNull(message = "must not be null") @Positive(message = "must be greater than 0") Double distanceKm,
        @NotNull(message = "must not be null") @PositiveOrZero(message = "must not be negative")
        Integer durationMinutes) {
}
