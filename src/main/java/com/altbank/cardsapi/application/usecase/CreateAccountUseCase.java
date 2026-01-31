package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.CreateAccountRequest;
import com.altbank.cardsapi.application.dto.CreateAccountResponse;
import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.application.port.CarrierPort;
import com.altbank.cardsapi.application.port.CustomerRepository;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.Address;
import com.altbank.cardsapi.domain.model.Customer;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.ReissueReason;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Objects;

@ApplicationScoped
public class CreateAccountUseCase {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PhysicalCardRepository physicalCardRepository;
    private final CarrierPort carrierPort;
    private final Clock clock;

    @Inject

    public CreateAccountUseCase(CustomerRepository customerRepository,
                                AccountRepository accountRepository,
                                PhysicalCardRepository physicalCardRepository,
                                CarrierPort carrierPort,
                                Clock clock) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
        this.physicalCardRepository = Objects.requireNonNull(physicalCardRepository, "physicalCardRepository");
        this.carrierPort = Objects.requireNonNull(carrierPort, "carrierPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public CreateAccountResponse create(CreateAccountRequest request) {
        String document = request.customer().document().trim();
        customerRepository.findByDocument(document).ifPresent(existing -> {
            throw new ConflictException(ErrorCode.CUSTOMER_ALREADY_EXISTS, "Customer already exists for document");
        });

        Address address = new Address(
                request.address().street(),
                request.address().number(),
                request.address().city(),
                request.address().state(),
                request.address().zipCode(),
                request.address().country()
        );

        Customer customer = new Customer(
                request.customer().fullName(),
                document,
                request.customer().email(),
                request.customer().phone(),
                address,
                clock
        );
        customerRepository.persist(customer);

        Account account = new Account(customer, clock);
        accountRepository.persist(account);

        CarrierPort.CarrierShipment shipment = carrierPort.createShipment(customer.address().asSingleLine());
        PhysicalCard physicalCard = new PhysicalCard(
                account,
                shipment.trackingId(),
                shipment.initialStatus(),
                customer.address().asSingleLine(),
                null,
                null,
                clock
        );
        physicalCardRepository.persist(physicalCard);

        return new CreateAccountResponse(customer.id(), account.id(), physicalCard.id(), physicalCard.trackingId());
    }
}
