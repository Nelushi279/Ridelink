package com.ridelink.account.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.LoginResponse;
import com.ridelink.account.exception.AccountNotActiveException;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.JwtService;

@Service
public class AccountLoginService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AccountLoginService(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
                               JwtService jwtService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (account.getPassword() == null || !passwordEncoder.matches(request.password(), account.getPassword())) {
            throw new InvalidCredentialsException();
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }

        return new LoginResponse(jwtService.issueToken(account), "Bearer",
                jwtService.getExpirationSeconds(), LoginResponse.AccountSummary.from(account));
    }
}
