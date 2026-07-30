package com.altbank.cardsapi.application.port;

import com.altbank.cardsapi.domain.model.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {
    Optional<Customer> findByDocument(String document);
    Optional<Customer> findById(UUID customerId);
    List<Customer> listAll();
    void persist(Customer customer);
}
