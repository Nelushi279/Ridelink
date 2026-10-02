package com.ridelink.account.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.exception.AccountNotActiveException;
import com.ridelink.account.exception.AccountNotFoundException;
import com.ridelink.account.exception.InvalidProfileException;
import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.repository.AccountRepository;

@Service
public class ProfileService {

    private final AccountRepository accountRepository;

    public ProfileService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse getOwnProfile(String accountId) {
        return AccountResponse.from(loadActiveAccount(accountId));
    }

    public AccountResponse updateOwnProfile(String accountId, UpdateProfileRequest request) {
        if (request.fullName() == null && request.phoneNumber() == null) {
            throw new InvalidProfileException();
        }

        Account account = loadActiveAccount(accountId);
        if (request.fullName() != null) {
            account.setFullName(request.fullName().trim());
        }
        if (request.phoneNumber() != null) {
            account.setPhoneNumber(request.phoneNumber().trim());
        }
        account.setUpdatedAt(Instant.now());

        return AccountResponse.from(accountRepository.save(account));
    }

    private Account loadActiveAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(AccountNotFoundException::new);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }
        return account;
    }
}
