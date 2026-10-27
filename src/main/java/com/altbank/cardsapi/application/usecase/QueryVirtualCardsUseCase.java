package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.PageRequest;
import com.altbank.cardsapi.application.dto.QueryResponses;
import com.altbank.cardsapi.application.dto.VirtualCardResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.application.security.OwnershipPolicy;
import com.altbank.cardsapi.domain.model.VirtualCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class QueryVirtualCardsUseCase {

    private final VirtualCardRepository virtualCardRepository;
    private final OwnershipPolicy ownershipPolicy;

    @Inject
    public QueryVirtualCardsUseCase(VirtualCardRepository virtualCardRepository, OwnershipPolicy ownershipPolicy) {
        this.virtualCardRepository = Objects.requireNonNull(virtualCardRepository, "virtualCardRepository");
        this.ownershipPolicy = Objects.requireNonNull(ownershipPolicy, "ownershipPolicy");
    }

    @Transactional
    public List<VirtualCardResponse> list(UUID accountId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        List<VirtualCard> cards = ownershipPolicy.isAdmin()
                ? listForAdmin(accountId, pageRequest)
                : listForOwner(accountId);
        return cards.stream().map(QueryResponses::toVirtualCard).toList();
    }

    private List<VirtualCard> listForAdmin(UUID accountId, PageRequest pageRequest) {
        return accountId == null
                ? virtualCardRepository.listAll(pageRequest.offset(), pageRequest.limit())
                : virtualCardRepository.listByAccountId(accountId);
    }

    private List<VirtualCard> listForOwner(UUID requestedAccountId) {
        Optional<UUID> ownedAccountId = ownershipPolicy.callerAccountId();
        if (ownedAccountId.isEmpty()) {
            return List.of();
        }
        UUID ownAccount = ownedAccountId.get();
        if (requestedAccountId != null && !requestedAccountId.equals(ownAccount)) {
            return List.of();
        }
        return virtualCardRepository.listByAccountId(ownAccount);
    }

    @Transactional
    public VirtualCardResponse get(UUID cardId) {
        VirtualCard card = virtualCardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.VIRTUAL_CARD_NOT_FOUND, "Virtual card not found"));
        if (!ownershipPolicy.canAccess(card.account())) {
            throw new NotFoundException(ErrorCode.VIRTUAL_CARD_NOT_FOUND, "Virtual card not found");
        }
        return QueryResponses.toVirtualCard(card);
    }
}
