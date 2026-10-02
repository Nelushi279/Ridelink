package com.ridelink.account.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.UpdateAccountStatusRequest;
import com.ridelink.account.exception.ApiError;
import com.ridelink.account.service.AccountStatusService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account administration")
@SecurityRequirement(name = "bearerAuth")
public class AccountStatusController {

    private final AccountStatusService accountStatusService;

    public AccountStatusController(AccountStatusService accountStatusService) {
        this.accountStatusService = accountStatusService;
    }

    @PatchMapping("/{accountId}/status")
    @Operation(summary = "Change an account's status",
            description = "Requires an authenticated account with the ADMIN role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account status updated",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status request",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public AccountResponse updateStatus(@AuthenticationPrincipal String administratorId,
                                        @PathVariable String accountId,
                                        @Valid @RequestBody UpdateAccountStatusRequest request) {
        return accountStatusService.updateStatus(administratorId, accountId, request);
    }
}
