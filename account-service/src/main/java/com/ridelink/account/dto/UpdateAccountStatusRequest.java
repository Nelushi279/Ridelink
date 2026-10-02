package com.ridelink.account.dto;

import com.ridelink.account.model.AccountStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "New status for an account")
public record UpdateAccountStatusRequest(
        @NotNull(message = "status is required")
        AccountStatus status
) {
}
