package com.ridelink.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ridelink.account.config.SecurityConfig;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.LoginResponse;
import com.ridelink.account.exception.AccountNotActiveException;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.security.JsonAuthenticationEntryPoint;
import com.ridelink.account.security.JwtService;
import com.ridelink.account.service.AccountLoginService;

@WebMvcTest(LoginController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class})
class LoginControllerTest {

    private static final String VALID_REQUEST = """
            {"email":"passenger@test.com","password":"Password123!"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AccountLoginService accountLoginService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void returnsBearerTokenAndSafeAccountSummary() throws Exception {
        when(accountLoginService.login(any(LoginRequest.class))).thenReturn(
                new LoginResponse("signed.jwt.token", "Bearer", 3600,
                        new LoginResponse.AccountSummary("account-1", "Test Passenger",
                                "passenger@test.com", AccountRole.PASSENGER, AccountStatus.ACTIVE)));

        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.account.id").value("account-1"))
                .andExpect(jsonPath("$.account.password").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void returnsGenericUnauthorizedResponseForBadCredentials() throws Exception {
        when(accountLoginService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void returnsForbiddenForNonActiveAccount() throws Exception {
        when(accountLoginService.login(any(LoginRequest.class))).thenThrow(new AccountNotActiveException());

        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is not active"));
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid-email","password":"Password123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        verifyNoInteractions(accountLoginService);
    }

    @Test
    void rejectsMissingPassword() throws Exception {
        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"passenger@test.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
        verifyNoInteractions(accountLoginService);
    }

    @Test
    void rejectsMalformedRequest() throws Exception {
        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }
}
