package com.ridelink.account.service;

import java.time.Instant;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.ridelink.account.dto.AccountResponse;
import com.ridelink.account.dto.UpdateAccountStatusRequest;
import com.ridelink.account.exception.AccountNotActiveException;
import com.ridelink.account.exception.AccountNotFoundException;
import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountRole;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.repository.AccountRepository;

@Service
public class AccountStatusService {

    private final AccountRepository accountRepository;

    public AccountStatusService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse updateStatus(String administratorId, String accountId,
                                        UpdateAccountStatusRequest request) {
        Account administrator = findAccount(administratorId);
        if (administrator.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }
        if (administrator.getRole() != AccountRole.ADMIN) {
            throw new AccessDeniedException("Administrator role required");
        }

        Account account = administratorId.equals(accountId) ? administrator : findAccount(accountId);
        account.setStatus(request.status());
        account.setUpdatedAt(Instant.now());

        return AccountResponse.from(accountRepository.save(account));
    }

    private Account findAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(AccountNotFoundException::new);
    }
}
