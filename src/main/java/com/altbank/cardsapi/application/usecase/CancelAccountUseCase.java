package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.CancelAccountResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
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
public class CancelAccountUseCase {

    private final AccountRepository accountRepository;
    private final PhysicalCardRepository physicalCardRepository;
    private final VirtualCardRepository virtualCardRepository;
    private final ProcessorPort processorPort;
    private final Clock clock;

    @Inject

    public CancelAccountUseCase(AccountRepository accountRepository,
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
    public CancelAccountResponse cancel(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found"));

        account.cancel(clock);

        physicalCardRepository.findActiveByAccountId(accountId).ifPresent(card -> card.deactivate(clock));
        virtualCardRepository.findActiveByAccountId(accountId).ifPresent(card -> {
            card.deactivate(clock);
            processorPort.deactivateCard(card.processorCardId());
        });

        return new CancelAccountResponse(account.id(), account.cancelledAt());
    }
}
