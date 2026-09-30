package com.ridelink.account.exception;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Consistent error response for account registration")
public record ApiError(
        Instant timestamp,
        int status,
        String message,
        Map<String, String> fieldErrors
) {
}
