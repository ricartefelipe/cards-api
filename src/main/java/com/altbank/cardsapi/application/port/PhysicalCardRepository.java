package com.altbank.cardsapi.application.port;

import com.altbank.cardsapi.domain.model.PhysicalCard;
import java.util.Optional;
import java.util.UUID;

public interface PhysicalCardRepository {
    Optional<PhysicalCard> findById(UUID cardId);
    Optional<PhysicalCard> findByTrackingId(String trackingId);
    Optional<PhysicalCard> findActiveByAccountId(UUID accountId);
    void persist(PhysicalCard physicalCard);
}
