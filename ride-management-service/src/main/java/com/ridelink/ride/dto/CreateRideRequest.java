package com.ridelink.ride.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRideRequest(
        @NotBlank(message = "must not be blank") String passengerAccountId,
        @NotNull(message = "must not be null") @Valid LocationDto pickupLocation,
        @NotNull(message = "must not be null") @Valid LocationDto dropoffLocation) {
}
