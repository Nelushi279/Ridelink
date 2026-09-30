package com.ridelink.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.RegisterAccountRequest;
import com.ridelink.account.exception.DuplicateEmailException;
import com.ridelink.account.exception.InvalidRegistrationException;
import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void savesActiveAccountWithNormalizedEmailAndBcryptPassword() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId("account-1");
            return saved;
        });
        AccountService service = new AccountService(accountRepository, passwordEncoder);
        RegisterAccountRequest request = new RegisterAccountRequest(" Test Passenger ",
                " Passenger@Test.com ", "Password123!", "0771234567", AccountRole.PASSENGER);

        AccountResponse response = service.register(request);

        ArgumentCaptor<Account> savedAccount = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).existsByEmail("passenger@test.com");
        verify(accountRepository).save(savedAccount.capture());
        Account account = savedAccount.getValue();
        assertEquals("account-1", response.id());
        assertEquals("Test Passenger", account.getFullName());
        assertEquals("passenger@test.com", account.getEmail());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals(account.getCreatedAt(), account.getUpdatedAt());
        assertNotEquals(request.password(), account.getPassword());
        assertTrue(passwordEncoder.matches(request.password(), account.getPassword()));
    }

    @Test
    void rejectsExistingEmailBeforeSaving() {
        when(accountRepository.existsByEmail("passenger@test.com")).thenReturn(true);
        AccountService service = new AccountService(accountRepository, passwordEncoder);

        assertThrows(DuplicateEmailException.class,
                () -> service.register(request(AccountRole.PASSENGER)));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void mapsUniqueIndexRaceToDuplicateEmail() {
        when(accountRepository.save(any(Account.class)))
                .thenThrow(new DuplicateKeyException("duplicate email"));
        AccountService service = new AccountService(accountRepository, passwordEncoder);

        assertThrows(DuplicateEmailException.class,
                () -> service.register(request(AccountRole.PASSENGER)));
    }

    @Test
    void rejectsPublicAdminRegistration() {
        AccountService service = new AccountService(accountRepository, passwordEncoder);

        assertThrows(InvalidRegistrationException.class,
                () -> service.register(request(AccountRole.ADMIN)));
        verify(accountRepository, never()).save(any(Account.class));
    }

    private RegisterAccountRequest request(AccountRole role) {
        return new RegisterAccountRequest("Test Passenger", "passenger@test.com",
                "Password123!", "0771234567", role);
    }
}
