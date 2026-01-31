package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.IssueVirtualCardResponse;
import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.AccountRepository;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import com.altbank.cardsapi.domain.model.VirtualCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class IssueVirtualCardUseCase {

    private final AccountRepository accountRepository;
    private final PhysicalCardRepository physicalCardRepository;
    private final VirtualCardRepository virtualCardRepository;
    private final ProcessorPort processorPort;
    private final Clock clock;

    @Inject

    public IssueVirtualCardUseCase(AccountRepository accountRepository,
                                   PhysicalCardRepository physicalCardRepository,
                                   VirtualCardRepository virtualCardRepository,
                                   ProcessorPort processorPort,
                                   Clock clock) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
        this.physicalCardRepository = Objects.requireNonNull(physicalCardRepository, "physicalCardRepository");
        this.virtualCardRepository = Objects.requireNonNull(virtualCardRepository, "virtualCardRepository");
        this.processorPort = Objects.requireNonNull(processorPort, "processorPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public IssueVirtualCardResponse issue(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found"));

        if (!account.isActive()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_CANCELLED, "Account is cancelled");
        }

        PhysicalCard physicalCard = physicalCardRepository.findActiveByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CARD_NOT_FOUND, "Active physical card not found"));

        if (!physicalCard.isDelivered()) {
            throw new ConflictException(ErrorCode.PHYSICAL_CARD_NOT_DELIVERED, "Physical card must be delivered before issuing virtual card");
        }
        if (!physicalCard.isValidated()) {
            throw new ConflictException(ErrorCode.PHYSICAL_CARD_NOT_VALIDATED, "Physical card must be validated before issuing virtual card");
        }

        virtualCardRepository.findActiveByAccountId(accountId).ifPresent(existing -> {
            throw new ConflictException(ErrorCode.VIRTUAL_CARD_ALREADY_EXISTS, "Active virtual card already exists for account");
        });

        String processorAccountId = account.id().toString();
        ProcessorPort.IssuedVirtualCard issued = processorPort.issueVirtualCard(processorAccountId);

        VirtualCard virtualCard = new VirtualCard(
                account,
                issued.processorAccountId(),
                issued.processorCardId(),
                issued.cvvExpirationAt(),
                clock
        );
        virtualCardRepository.persist(virtualCard);

        return new IssueVirtualCardResponse(virtualCard.id(), virtualCard.processorCardId(), virtualCard.cvvExpirationAt());
    }
}
