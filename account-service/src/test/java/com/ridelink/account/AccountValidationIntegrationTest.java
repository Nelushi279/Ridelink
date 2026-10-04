package com.ridelink.account;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.repository.AccountRepository;

@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.data.mongodb.uri=mongodb://localhost:27017/ridelink_account_test",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
})
@AutoConfigureMockMvc
class AccountValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountRepository accountRepository;

    @ParameterizedTest
    @CsvSource({
            "PASSENGER, ACTIVE",
            "DRIVER, INACTIVE",
            "ADMIN, SUSPENDED"
    })
    void existingAccountReturnsOnlySafeValidationFacts(AccountRole role, AccountStatus accountStatus)
            throws Exception {
        Instant createdAt = Instant.parse("2026-10-01T08:00:00Z");
        Instant updatedAt = Instant.parse("2026-10-02T08:00:00Z");
        Account account = account(role, accountStatus, createdAt, updatedAt);
        when(accountRepository.findById("account-123")).thenReturn(Optional.of(account));

        mockMvc.perform(get("/api/accounts/account-123/validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("account-123"))
                .andExpect(jsonPath("$.role").value(role.name()))
                .andExpect(jsonPath("$.status").value(accountStatus.name()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.fullName").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(accountRepository).findById("account-123");
        verify(accountRepository, never()).save(any(Account.class));
        assertEquals(createdAt, account.getCreatedAt());
        assertEquals(updatedAt, account.getUpdatedAt());
    }

    @Test
    void unknownAccountReturnsNotFound() throws Exception {
        when(accountRepository.findById("missing-account")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/missing-account/validation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found"));

        verify(accountRepository).findById("missing-account");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void swaggerDocumentsAccountValidationEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/accounts/{accountId}/validation")))
                .andExpect(content().string(containsString("AccountValidationResponse")));
    }

    private Account account(AccountRole role, AccountStatus status, Instant createdAt, Instant updatedAt) {
        Account account = new Account("Private Name", "private@test.com", "bcrypt-password-hash",
                "0771234567", role, status, createdAt, updatedAt);
        account.setId("account-123");
        return account;
    }
}
