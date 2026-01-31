package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.ValidatePhysicalCardResponse;
import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class ValidatePhysicalCardUseCase {

    private final PhysicalCardRepository physicalCardRepository;
    private final Clock clock;

    @Inject

    public ValidatePhysicalCardUseCase(PhysicalCardRepository physicalCardRepository, Clock clock) {
        this.physicalCardRepository = Objects.requireNonNull(physicalCardRepository, "physicalCardRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public ValidatePhysicalCardResponse validate(UUID physicalCardId) {
        PhysicalCard card = physicalCardRepository.findById(physicalCardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CARD_NOT_FOUND, "Physical card not found"));

        Account account = card.account();
        if (!account.isActive()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_CANCELLED, "Account is cancelled");
        }
        if (!card.isActive()) {
            throw new ForbiddenException(ErrorCode.CARD_INACTIVE, "Card is inactive");
        }
        if (!card.isDelivered()) {
            throw new ConflictException(ErrorCode.PHYSICAL_CARD_NOT_DELIVERED, "Physical card must be delivered before validation");
        }

        card.validate(clock);
        return new ValidatePhysicalCardResponse(card.id(), card.validatedAt());
    }
}
