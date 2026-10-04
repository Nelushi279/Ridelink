package com.ridelink.account.dto;

import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Minimal account facts for internal RideLink service integration")
public record AccountValidationResponse(
        String accountId,
        AccountRole role,
        AccountStatus status
) {
    public static AccountValidationResponse from(Account account) {
        return new AccountValidationResponse(account.getId(), account.getRole(), account.getStatus());
    }
}
