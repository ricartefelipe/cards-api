package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.dto.QueryResponses;
import com.altbank.cardsapi.application.dto.VirtualCardResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.VirtualCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class QueryVirtualCardsUseCase {

    private final VirtualCardRepository virtualCardRepository;

    @Inject
    public QueryVirtualCardsUseCase(VirtualCardRepository virtualCardRepository) {
        this.virtualCardRepository = Objects.requireNonNull(virtualCardRepository, "virtualCardRepository");
    }

    @Transactional
    public List<VirtualCardResponse> list(UUID accountId) {
        List<VirtualCard> cards = accountId == null
                ? virtualCardRepository.listAll()
                : virtualCardRepository.listByAccountId(accountId);
        return cards.stream().map(QueryResponses::toVirtualCard).toList();
    }

    @Transactional
    public VirtualCardResponse get(UUID cardId) {
        VirtualCard card = virtualCardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.VIRTUAL_CARD_NOT_FOUND, "Virtual card not found"));
        return QueryResponses.toVirtualCard(card);
    }
}
