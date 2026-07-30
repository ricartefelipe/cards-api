package com.altbank.cardsapi.infrastructure.persistence;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.domain.model.Account;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaAccountRepository implements AccountRepository {

    private final EntityManager entityManager;

    @Inject
    public JpaAccountRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return Optional.ofNullable(entityManager.find(Account.class, accountId));
    }

    @Override
    public Optional<Account> findByCustomerId(UUID customerId) {
        return entityManager.createQuery("SELECT a FROM Account a WHERE a.customer.id = :customerId", Account.class)
                .setParameter("customerId", customerId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Account> listAll() {
        return entityManager.createQuery(
                        "SELECT a FROM Account a JOIN FETCH a.customer ORDER BY a.createdAt DESC",
                        Account.class
                )
                .getResultList();
    }

    @Override
    public void persist(Account account) {
        entityManager.persist(account);
    }
}
