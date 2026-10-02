package com.ridelink.ride.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignDriverRequest(
        @NotBlank(message = "must not be blank") String driverId) {
}
