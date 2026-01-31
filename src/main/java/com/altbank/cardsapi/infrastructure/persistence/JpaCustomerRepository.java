package com.altbank.cardsapi.infrastructure.persistence;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.port.CustomerRepository;
import com.altbank.cardsapi.domain.model.Customer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class JpaCustomerRepository implements CustomerRepository {

    private final EntityManager entityManager;

    @Inject

    public JpaCustomerRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<Customer> findByDocument(String document) {
        return entityManager.createQuery("SELECT c FROM Customer c WHERE c.document = :document", Customer.class)
                .setParameter("document", document)
                .getResultStream()
                .findFirst();
    }

    @Override
    public void persist(Customer customer) {
        entityManager.persist(customer);
    }
}
