package com.ridelink.ride.integration.dto;

public record AccountValidationResponse(
        String accountId,
        String role,
        String status) {
}
