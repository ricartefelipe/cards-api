package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.PhysicalCardResponse;
import com.altbank.cardsapi.application.dto.QueryResponses;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class QueryPhysicalCardsUseCase {

    private final PhysicalCardRepository physicalCardRepository;

    @Inject
    public QueryPhysicalCardsUseCase(PhysicalCardRepository physicalCardRepository) {
        this.physicalCardRepository = Objects.requireNonNull(physicalCardRepository, "physicalCardRepository");
    }

    @Transactional
    public List<PhysicalCardResponse> list(UUID accountId) {
        List<PhysicalCard> cards = accountId == null
                ? physicalCardRepository.listAll()
                : physicalCardRepository.listByAccountId(accountId);
        return cards.stream().map(QueryResponses::toPhysicalCard).toList();
    }

    @Transactional
    public PhysicalCardResponse get(UUID cardId) {
        PhysicalCard card = physicalCardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PHYSICAL_CARD_NOT_FOUND, "Physical card not found"));
        return QueryResponses.toPhysicalCard(card);
    }
}
