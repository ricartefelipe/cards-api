package com.altbank.cardsapi.application.usecase;

import com.altbank.cardsapi.application.PageRequest;
import com.altbank.cardsapi.application.dto.PhysicalCardResponse;
import com.altbank.cardsapi.application.dto.QueryResponses;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.security.OwnershipPolicy;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class QueryPhysicalCardsUseCase {

    private final PhysicalCardRepository physicalCardRepository;
    private final OwnershipPolicy ownershipPolicy;

    @Inject
    public QueryPhysicalCardsUseCase(PhysicalCardRepository physicalCardRepository, OwnershipPolicy ownershipPolicy) {
        this.physicalCardRepository = Objects.requireNonNull(physicalCardRepository, "physicalCardRepository");
        this.ownershipPolicy = Objects.requireNonNull(ownershipPolicy, "ownershipPolicy");
    }

    @Transactional
    public List<PhysicalCardResponse> list(UUID accountId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        List<PhysicalCard> cards = ownershipPolicy.isAdmin()
                ? listForAdmin(accountId, pageRequest)
                : listForOwner(accountId);
        return cards.stream().map(QueryResponses::toPhysicalCard).toList();
    }

    private List<PhysicalCard> listForAdmin(UUID accountId, PageRequest pageRequest) {
        return accountId == null
                ? physicalCardRepository.listAll(pageRequest.offset(), pageRequest.limit())
                : physicalCardRepository.listByAccountId(accountId);
    }

    private List<PhysicalCard> listForOwner(UUID requestedAccountId) {
        Optional<UUID> ownedAccountId = ownershipPolicy.callerAccountId();
        if (ownedAccountId.isEmpty()) {
            return List.of();
        }
        UUID ownAccount = ownedAccountId.get();
        if (requestedAccountId != null && !requestedAccountId.equals(ownAccount)) {
            return List.of();
        }
        return physicalCardRepository.listByAccountId(ownAccount);
    }

    @Transactional
    public PhysicalCardResponse get(UUID cardId) {
        PhysicalCard card = physicalCardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PHYSICAL_CARD_NOT_FOUND, "Physical card not found"));
        if (!ownershipPolicy.canAccess(card.account())) {
            throw new NotFoundException(ErrorCode.PHYSICAL_CARD_NOT_FOUND, "Physical card not found");
        }
        return QueryResponses.toPhysicalCard(card);
    }
}
