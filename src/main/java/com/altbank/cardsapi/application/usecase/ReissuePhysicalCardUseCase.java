package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.ReissuePhysicalCardRequest;
import com.altbank.cardsapi.application.dto.ReissuePhysicalCardResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.TransactionRunner;
import com.altbank.cardsapi.application.shipping.ShipmentOrchestrator;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.application.security.OwnershipPolicy;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.Customer;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class ReissuePhysicalCardUseCase {

    static final String ACTOR = "card-reissue";

    private final PhysicalCardRepository physicalCardRepository;
    private final ShipmentOrchestrator shipments;
    private final TransactionRunner tx;
    private final OwnershipPolicy ownershipPolicy;
    private final Clock clock;

    @Inject
    public ReissuePhysicalCardUseCase(PhysicalCardRepository physicalCardRepository,
                                      ShipmentOrchestrator shipments,
                                      TransactionRunner tx,
                                      OwnershipPolicy ownershipPolicy,
                                      Clock clock) {
        this.physicalCardRepository = Objects.requireNonNull(physicalCardRepository, "physicalCardRepository");
        this.shipments = Objects.requireNonNull(shipments, "shipments");
        this.tx = Objects.requireNonNull(tx, "tx");
        this.ownershipPolicy = Objects.requireNonNull(ownershipPolicy, "ownershipPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    private record Reissued(UUID oldCardId, UUID newCardId, UUID shipmentRequestId) {
    }

    // Desativacao, cartao novo e intencao de remessa na mesma transacao; a transportadora so e
    // chamada depois, fora dela e do lock do cartao.
    public ReissuePhysicalCardResponse reissue(UUID physicalCardId, ReissuePhysicalCardRequest request) {
        Reissued reissued = tx.inTransaction(() -> persist(physicalCardId, request));
        ShipmentOrchestrator.Dispatch dispatch = shipments.dispatch(reissued.shipmentRequestId(), ACTOR);
        return new ReissuePhysicalCardResponse(
                reissued.oldCardId(), reissued.newCardId(), dispatch.trackingId().orElse(null), dispatch.status().name());
    }

    private Reissued persist(UUID physicalCardId, ReissuePhysicalCardRequest request) {
        PhysicalCard oldCard = physicalCardRepository.findById(physicalCardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CARD_NOT_FOUND, "Physical card not found"));

        Account account = oldCard.account();
        if (!ownershipPolicy.canAccess(account)) {
            throw new NotFoundException(ErrorCode.CARD_NOT_FOUND, "Physical card not found");
        }
        if (!account.isActive()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_CANCELLED, "Account is cancelled");
        }
        if (!oldCard.isActive()) {
            throw new ForbiddenException(ErrorCode.CARD_INACTIVE, "Card is inactive");
        }

        oldCard.deactivate(clock);

        Customer customer = account.customer();
        String address = customer.address().asSingleLine();
        PhysicalCard newCard = new PhysicalCard(account, address, request.reason(), oldCard, clock);
        physicalCardRepository.persist(newCard);
        UUID shipmentRequestId = shipments.requestShipment(newCard, address, ACTOR);

        return new Reissued(oldCard.id(), newCard.id(), shipmentRequestId);
    }
}
