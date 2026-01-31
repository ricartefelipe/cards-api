package com.altbank.cardsapi.application.port;

import com.altbank.cardsapi.domain.model.Customer;
import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findByDocument(String document);
    void persist(Customer customer);
}
