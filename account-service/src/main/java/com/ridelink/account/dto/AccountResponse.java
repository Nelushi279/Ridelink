package com.ridelink.account.dto;

import java.time.Instant;

import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered account details. The password is never returned.")
public record AccountResponse(
        String id,
        String fullName,
        String email,
        String phoneNumber,
        AccountRole role,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getFullName(), account.getEmail(),
                account.getPhoneNumber(), account.getRole(), account.getStatus(),
                account.getCreatedAt(), account.getUpdatedAt());
    }
}
