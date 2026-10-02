package com.ridelink.account;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.data.mongodb.uri=mongodb://localhost:27017/ridelink_account_test",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@AutoConfigureMockMvc
class AccountStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AccountRepository accountRepository;

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void administratorCanSetEverySupportedStatus(AccountStatus requestedStatus) throws Exception {
        Account administrator = account("admin-1", AccountRole.ADMIN, AccountStatus.ACTIVE);
        Account target = account("account-1", AccountRole.PASSENGER, AccountStatus.ACTIVE);
        when(accountRepository.findById("admin-1")).thenReturn(Optional.of(administrator));
        when(accountRepository.findById("account-1")).thenReturn(Optional.of(target));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/accounts/account-1/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(administrator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + requestedStatus + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("account-1"))
                .andExpect(jsonPath("$.status").value(requestedStatus.name()))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertEquals(requestedStatus, target.getStatus());
        assertTrue(target.getUpdatedAt().isAfter(target.getCreatedAt()));
    }

    @Test
    void unknownTargetAccountReturnsNotFound() throws Exception {
        Account administrator = account("admin-1", AccountRole.ADMIN, AccountStatus.ACTIVE);
        when(accountRepository.findById("admin-1")).thenReturn(Optional.of(administrator));
        when(accountRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/accounts/missing/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(administrator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void missingAndInvalidTokensReturnUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/accounts/account-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(patch("/api/accounts/account-1/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @EnumSource(value = AccountRole.class, names = {"PASSENGER", "DRIVER"})
    void nonAdministratorsReceiveForbidden(AccountRole role) throws Exception {
        Account caller = account("caller-1", role, AccountStatus.ACTIVE);

        mockMvc.perform(patch("/api/accounts/account-1/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(caller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void invalidAndMissingStatusReturnBadRequest() throws Exception {
        Account administrator = account("admin-1", AccountRole.ADMIN, AccountStatus.ACTIVE);
        String bearer = bearer(administrator);

        mockMvc.perform(patch("/api/accounts/account-1/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(patch("/api/accounts/account-1/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").value("status is required"));
    }

    @Test
    void administratorWithInactiveCurrentAccountReceivesForbidden() throws Exception {
        Account tokenAccount = account("admin-1", AccountRole.ADMIN, AccountStatus.ACTIVE);
        Account currentAccount = account("admin-1", AccountRole.ADMIN, AccountStatus.SUSPENDED);
        when(accountRepository.findById("admin-1")).thenReturn(Optional.of(currentAccount));

        mockMvc.perform(patch("/api/accounts/account-1/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAccount))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is not active"));
    }

    @Test
    void swaggerDocumentsAdministrativeStatusEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/accounts/{accountId}/status")))
                .andExpect(content().string(containsString("ADMIN role")))
                .andExpect(content().string(containsString("bearerAuth")));
    }

    private String bearer(Account account) {
        return "Bearer " + jwtService.issueToken(account);
    }

    private Account account(String id, AccountRole role, AccountStatus status) {
        Instant createdAt = Instant.parse("2026-10-01T08:00:00Z");
        Account account = new Account("Test Account", id + "@test.com", "bcrypt-password-hash",
                "0771234567", role, status, createdAt, createdAt);
        account.setId(id);
        return account;
    }
}
