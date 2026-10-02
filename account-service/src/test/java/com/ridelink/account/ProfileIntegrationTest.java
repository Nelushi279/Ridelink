package com.ridelink.account;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.JwtService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.data.mongodb.uri=mongodb://localhost:27017/ridelink_account_test",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@AutoConfigureMockMvc
class ProfileIntegrationTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Test
    void authenticatedAccountRetrievesOnlyOwnSafeProfile() throws Exception {
        Account account = account();
        when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));

        mockMvc.perform(get("/api/accounts/me")
                        .param("accountId", "another-account")
                        .header(HttpHeaders.AUTHORIZATION, bearer(account)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("account-1"))
                .andExpect(jsonPath("$.fullName").value("Test Passenger"))
                .andExpect(jsonPath("$.email").value("passenger@test.com"))
                .andExpect(jsonPath("$.phoneNumber").value("0771234567"))
                .andExpect(jsonPath("$.role").value("PASSENGER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(accountRepository).findById("account-1");
    }

    @Test
    void missingInvalidAndExpiredTokensReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void authenticatedAccountUpdatesFullName() throws Exception {
        Account account = account();
        mockSuccessfulSave(account);

        mockMvc.perform(patch("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(account))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\" Nelushi Balasuriya \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nelushi Balasuriya"))
                .andExpect(jsonPath("$.phoneNumber").value("0771234567"));

        assertEquals("Nelushi Balasuriya", account.getFullName());
        assertTrue(account.getUpdatedAt().isAfter(account.getCreatedAt()));
    }

    @Test
    void authenticatedAccountUpdatesPhoneNumber() throws Exception {
        Account account = account();
        mockSuccessfulSave(account);

        mockMvc.perform(patch("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(account))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+94771234567\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+94771234567"))
                .andExpect(jsonPath("$.fullName").value("Test Passenger"));
    }

    @Test
    void invalidAndEmptyUpdatesReturnBadRequest() throws Exception {
        Account account = account();
        String bearer = bearer(account);

        mockMvc.perform(patch("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());

        mockMvc.perform(patch("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists());

        mockMvc.perform(patch("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("At least one profile field must be supplied"));
    }

    @Test
    void protectedFieldsCannotBeUpdated() throws Exception {
        Account account = account();
        String originalPassword = account.getPassword();
        mockSuccessfulSave(account);

        mockMvc.perform(patch("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(account))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"New Name","id":"another-account","email":"other@test.com",
                                 "password":"changed","role":"ADMIN","status":"SUSPENDED",
                                 "createdAt":"2020-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("account-1"))
                .andExpect(jsonPath("$.email").value("passenger@test.com"))
                .andExpect(jsonPath("$.role").value("PASSENGER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertEquals(originalPassword, account.getPassword());
        assertEquals(Instant.parse("2026-10-01T08:00:00Z"), account.getCreatedAt());
    }

    @Test
    void deletedJwtAccountReturnsNotFound() throws Exception {
        Account account = account();
        when(accountRepository.findById("account-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(account)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void inactiveAccountReturnsForbidden() throws Exception {
        Account tokenAccount = account();
        Account inactiveAccount = account(AccountStatus.INACTIVE);
        when(accountRepository.findById("account-1")).thenReturn(Optional.of(inactiveAccount));

        mockMvc.perform(get("/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccount)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is not active"));
    }

    @Test
    void swaggerDocumentsProtectedProfileEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/accounts/me")))
                .andExpect(content().string(containsString("bearerAuth")));
    }

    private void mockSuccessfulSave(Account account) {
        when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private String bearer(Account account) {
        return "Bearer " + jwtService.issueToken(account);
    }

    private String expiredToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("ridelink-account-service")
                .subject("account-1")
                .issuedAt(Date.from(now.minusSeconds(3600)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET)), Jwts.SIG.HS256)
                .compact();
    }

    private Account account() {
        return account(AccountStatus.ACTIVE);
    }

    private Account account(AccountStatus status) {
        Instant createdAt = Instant.parse("2026-10-01T08:00:00Z");
        Account account = new Account("Test Passenger", "passenger@test.com", "bcrypt-password-hash",
                "0771234567", AccountRole.PASSENGER, status, createdAt, createdAt);
        account.setId("account-1");
        return account;
    }
}
