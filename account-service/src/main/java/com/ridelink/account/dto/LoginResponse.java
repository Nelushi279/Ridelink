package com.ridelink.account.dto;

import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Signed access token and safe account summary")
public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        AccountSummary account
) {
    public record AccountSummary(
            String id,
            String fullName,
            String email,
            AccountRole role,
            AccountStatus status
    ) {
        public static AccountSummary from(Account account) {
            return new AccountSummary(account.getId(), account.getFullName(), account.getEmail(),
                    account.getRole(), account.getStatus());
        }
    }
}
