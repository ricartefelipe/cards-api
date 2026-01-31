package com.altbank.cardsapi.application.port;

import com.altbank.cardsapi.domain.model.Account;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID accountId);
    Optional<Account> findByCustomerId(UUID customerId);
    void persist(Account account);
}
