package com.ridelink.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.LoginResponse;
import com.ridelink.account.exception.AccountNotActiveException;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AccountLoginServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void logsInActiveAccountAndReturnsTokenWithoutPassword() {
        Account account = account(AccountStatus.ACTIVE);
        when(accountRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(account));
        when(jwtService.issueToken(account)).thenReturn("signed.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        AccountLoginService service = service();

        LoginResponse response = service.login(new LoginRequest(" Passenger@Test.com ", "Password123!"));

        verify(accountRepository).findByEmail("passenger@test.com");
        assertEquals("signed.jwt.token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertEquals("account-1", response.account().id());
        assertEquals(AccountRole.PASSENGER, response.account().role());
        assertEquals(AccountStatus.ACTIVE, response.account().status());
        assertEquals("passenger@test.com", response.account().email());
    }

    @Test
    void unknownEmailUsesGenericCredentialError() {
        when(accountRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> service().login(new LoginRequest("missing@test.com", "Password123!")));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtService, never()).issueToken(org.mockito.ArgumentMatchers.any(Account.class));
    }

    @Test
    void wrongPasswordUsesSameGenericCredentialError() {
        Account account = account(AccountStatus.ACTIVE);
        when(accountRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(account));

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> service().login(new LoginRequest("passenger@test.com", "WrongPassword!")));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtService, never()).issueToken(org.mockito.ArgumentMatchers.any(Account.class));
    }

    @Test
    void inactiveAccountCannotLogIn() {
        assertBlocked(AccountStatus.INACTIVE);
    }

    @Test
    void suspendedAccountCannotLogIn() {
        assertBlocked(AccountStatus.SUSPENDED);
    }

    private void assertBlocked(AccountStatus status) {
        Account account = account(status);
        when(accountRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(account));

        assertThrows(AccountNotActiveException.class,
                () -> service().login(new LoginRequest("passenger@test.com", "Password123!")));
        verify(jwtService, never()).issueToken(org.mockito.ArgumentMatchers.any(Account.class));
    }

    private AccountLoginService service() {
        return new AccountLoginService(accountRepository, passwordEncoder, jwtService);
    }

    private Account account(AccountStatus status) {
        Instant now = Instant.parse("2026-09-30T10:00:00Z");
        Account account = new Account("Test Passenger", "passenger@test.com",
                passwordEncoder.encode("Password123!"), "0771234567", AccountRole.PASSENGER,
                status, now, now);
        account.setId("account-1");
        return account;
    }
}
