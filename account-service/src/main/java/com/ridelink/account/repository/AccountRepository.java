package com.ridelink.account.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ridelink.account.model.Account;

public interface AccountRepository extends MongoRepository<Account, String> {

    boolean existsByEmail(String email);
}
