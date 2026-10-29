package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.audit.AuditAction;
import com.altbank.cardsapi.application.audit.AuditService;
import com.altbank.cardsapi.application.dto.CreateAccountRequest;
import com.altbank.cardsapi.application.dto.CreateAccountResponse;
import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.application.port.TransactionRunner;
import com.altbank.cardsapi.application.shipping.ShipmentOrchestrator;
import com.altbank.cardsapi.application.port.CustomerRepository;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.Address;
import com.altbank.cardsapi.domain.model.Customer;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class CreateAccountUseCase {

    static final String ACTOR = "account-create";

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PhysicalCardRepository physicalCardRepository;
    private final ShipmentOrchestrator shipments;
    private final TransactionRunner tx;
    private final AuditService auditService;
    private final Clock clock;

    @Inject
    public CreateAccountUseCase(CustomerRepository customerRepository,
                                AccountRepository accountRepository,
                                PhysicalCardRepository physicalCardRepository,
                                ShipmentOrchestrator shipments,
                                TransactionRunner tx,
                                AuditService auditService,
                                Clock clock) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
        this.physicalCardRepository = Objects.requireNonNull(physicalCardRepository, "physicalCardRepository");
        this.shipments = Objects.requireNonNull(shipments, "shipments");
        this.tx = Objects.requireNonNull(tx, "tx");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    private record Created(UUID customerId, UUID accountId, UUID cardId, UUID shipmentRequestId) {
    }

    // A intencao de remessa e gravada junto com o cartao; a chamada a transportadora vem depois
    // da transacao, para que uma falha dela nao desfaca a conta nem fique sem registro.
    public CreateAccountResponse create(CreateAccountRequest request) {
        Created created = tx.inTransaction(() -> persist(request));
        ShipmentOrchestrator.Dispatch dispatch = shipments.dispatch(created.shipmentRequestId(), ACTOR);
        return new CreateAccountResponse(
                created.customerId(), created.accountId(), created.cardId(),
                dispatch.trackingId().orElse(null), dispatch.status().name());
    }

    private Created persist(CreateAccountRequest request) {
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
                request.customer().subject(),
                clock
        );
        customerRepository.persist(customer);

        Account account = new Account(customer, clock);
        accountRepository.persist(account);

        String deliveryAddress = customer.address().asSingleLine();
        PhysicalCard physicalCard = new PhysicalCard(account, deliveryAddress, null, null, clock);
        physicalCardRepository.persist(physicalCard);
        UUID shipmentRequestId = shipments.requestShipment(physicalCard, deliveryAddress, ACTOR);

        auditService.record(AuditAction.CREATE_ACCOUNT, "account", account.id());

        return new Created(customer.id(), account.id(), physicalCard.id(), shipmentRequestId);
    }
}
