package com.ridelink.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.RegisterAccountRequest;
import com.ridelink.account.config.SecurityConfig;
import com.ridelink.account.exception.DuplicateEmailException;
import com.ridelink.account.security.JsonAuthenticationEntryPoint;
import com.ridelink.account.security.JwtService;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.service.AccountService;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class})
class AccountControllerTest {

    private static final String VALID_REQUEST = """
            {
              "fullName": "Test Passenger",
              "email": "passenger@test.com",
              "password": "Password123!",
              "phoneNumber": "0771234567",
              "role": "PASSENGER"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void returnsCreatedAccountWithoutPassword() throws Exception {
        Instant createdAt = Instant.parse("2026-09-30T10:00:00Z");
        when(accountService.register(any(RegisterAccountRequest.class)))
                .thenReturn(new AccountResponse("account-1", "Test Passenger", "passenger@test.com",
                        "0771234567", AccountRole.PASSENGER, AccountStatus.ACTIVE, createdAt, createdAt));

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("account-1"))
                .andExpect(jsonPath("$.email").value("passenger@test.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void returnsConflictForDuplicateEmail() throws Exception {
        when(accountService.register(any(RegisterAccountRequest.class)))
                .thenThrow(new DuplicateEmailException());

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An account with this email already exists"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void rejectsInvalidEmailBeforeCallingService() throws Exception {
        String request = VALID_REQUEST.replace("passenger@test.com", "not-an-email");

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        verifyNoInteractions(accountService);
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
        verifyNoInteractions(accountService);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void hidesUnexpectedErrorDetails() throws Exception {
        when(accountService.register(any(RegisterAccountRequest.class)))
                .thenThrow(new IllegalStateException("internal detail"));

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
