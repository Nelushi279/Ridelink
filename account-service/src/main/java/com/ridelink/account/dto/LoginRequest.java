package com.ridelink.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Email and password used to log in")
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
