package com.altbank.cardsapi.application.port;

import com.altbank.cardsapi.domain.model.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID accountId);
    Optional<Account> findByCustomerId(UUID customerId);
    List<Account> listAll();
    void persist(Account account);
}
