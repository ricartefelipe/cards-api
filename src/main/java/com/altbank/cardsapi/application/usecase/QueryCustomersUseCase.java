package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.CustomerDetailResponse;
import com.altbank.cardsapi.application.dto.CustomerSummaryResponse;
import com.altbank.cardsapi.application.dto.QueryResponses;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.application.port.CustomerRepository;
import com.altbank.cardsapi.domain.model.Customer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class QueryCustomersUseCase {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    @Inject
    public QueryCustomersUseCase(CustomerRepository customerRepository, AccountRepository accountRepository) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
    }

    @Transactional
    public List<CustomerSummaryResponse> list() {
        return customerRepository.listAll().stream().map(QueryResponses::toCustomerSummary).toList();
    }

    @Transactional
    public CustomerDetailResponse get(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found"));
        UUID accountId = accountRepository.findByCustomerId(customerId).map(a -> a.id()).orElse(null);
        return QueryResponses.toCustomerDetail(customer, accountId);
    }
}
