package com.ridelink.account.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.RegisterAccountRequest;
import com.ridelink.account.exception.DuplicateEmailException;
import com.ridelink.account.exception.InvalidRegistrationException;
import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.repository.AccountRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AccountResponse register(RegisterAccountRequest request) {
        if (request.role() == AccountRole.ADMIN) {
            throw new InvalidRegistrationException("ADMIN accounts cannot be registered publicly");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (accountRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        Instant now = Instant.now();
        Account account = new Account(request.fullName().trim(), email,
                passwordEncoder.encode(request.password()), request.phoneNumber().trim(),
                request.role(), AccountStatus.ACTIVE, now, now);

        try {
            return AccountResponse.from(accountRepository.save(account));
        } catch (DuplicateKeyException exception) {
            // The unique email index also protects concurrent registration requests.
            throw new DuplicateEmailException(exception);
        }
    }
}
