package com.altbank.cardsapi.application.usecase;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.dto.ReissuePhysicalCardRequest;
import com.altbank.cardsapi.application.dto.ReissuePhysicalCardResponse;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.exception.ForbiddenException;
import com.altbank.cardsapi.application.exception.NotFoundException;
import com.altbank.cardsapi.application.port.CarrierPort;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.domain.model.Account;
import com.altbank.cardsapi.domain.model.Customer;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class ReissuePhysicalCardUseCase {

    private final PhysicalCardRepository physicalCardRepository;
    private final CarrierPort carrierPort;
    private final Clock clock;

    @Inject

    public ReissuePhysicalCardUseCase(PhysicalCardRepository physicalCardRepository, CarrierPort carrierPort, Clock clock) {
        this.physicalCardRepository = Objects.requireNonNull(physicalCardRepository, "physicalCardRepository");
        this.carrierPort = Objects.requireNonNull(carrierPort, "carrierPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public ReissuePhysicalCardResponse reissue(UUID physicalCardId, ReissuePhysicalCardRequest request) {
        PhysicalCard oldCard = physicalCardRepository.findById(physicalCardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CARD_NOT_FOUND, "Physical card not found"));

        Account account = oldCard.account();
        if (!account.isActive()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_CANCELLED, "Account is cancelled");
        }
        if (!oldCard.isActive()) {
            throw new ForbiddenException(ErrorCode.CARD_INACTIVE, "Card is inactive");
        }

        oldCard.deactivate(clock);

        Customer customer = account.customer();
        CarrierPort.CarrierShipment shipment = carrierPort.createShipment(customer.address().asSingleLine());

        PhysicalCard newCard = new PhysicalCard(
                account,
                shipment.trackingId(),
                shipment.initialStatus(),
                customer.address().asSingleLine(),
                request.reason(),
                oldCard,
                clock
        );
        physicalCardRepository.persist(newCard);

        return new ReissuePhysicalCardResponse(oldCard.id(), newCard.id(), newCard.trackingId());
    }
}
