package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.ReissueVirtualCardRequest;
import com.altbank.cardsapi.application.dto.ReissueVirtualCardResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.ProcessorPort;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.VirtualCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class ReissueVirtualCardUseCase {

    private final VirtualCardRepository virtualCardRepository;
    private final ProcessorPort processorPort;
    private final Clock clock;

    @Inject

    public ReissueVirtualCardUseCase(VirtualCardRepository virtualCardRepository, ProcessorPort processorPort, Clock clock) {
        this.virtualCardRepository = Objects.requireNonNull(virtualCardRepository, "virtualCardRepository");
        this.processorPort = Objects.requireNonNull(processorPort, "processorPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public ReissueVirtualCardResponse reissue(UUID virtualCardId, ReissueVirtualCardRequest request) {
        VirtualCard oldCard = virtualCardRepository.findById(virtualCardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.VIRTUAL_CARD_NOT_FOUND, "Virtual card not found"));

        Account account = oldCard.account();
        if (!account.isActive()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_CANCELLED, "Account is cancelled");
        }
        if (!oldCard.isActive()) {
            throw new ForbiddenException(ErrorCode.CARD_INACTIVE, "Card is inactive");
        }

        oldCard.deactivate(clock);
        processorPort.deactivateCard(oldCard.processorCardId());

        ProcessorPort.IssuedVirtualCard issued = processorPort.issueVirtualCard(oldCard.processorAccountId());

        VirtualCard newCard = new VirtualCard(
                account,
                issued.processorAccountId(),
                issued.processorCardId(),
                issued.cvvExpirationAt(),
                request.reason(),
                oldCard,
                clock
        );
        virtualCardRepository.persist(newCard);

        return new ReissueVirtualCardResponse(oldCard.id(), newCard.id(), newCard.processorCardId(), newCard.cvvExpirationAt());
    }
}
