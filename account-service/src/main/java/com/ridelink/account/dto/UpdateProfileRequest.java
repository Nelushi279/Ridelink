package com.ridelink.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Profile fields to update; at least one field is required")
public record UpdateProfileRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
        @Size(max = 100)
        String fullName,

        @Pattern(regexp = "\\+?[0-9]{7,15}",
                message = "must contain 7 to 15 digits, optionally starting with +")
        String phoneNumber
) {
}
