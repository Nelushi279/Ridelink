package com.ridelink.account.dto;

import com.ridelink.account.model.AccountRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Details needed to register a passenger or driver account")
public record RegisterAccountRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{7,15}", message = "must contain 7 to 15 digits, optionally starting with +") String phoneNumber,
        @NotNull AccountRole role
) {
}
