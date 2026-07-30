package com.altbank.cardsapi.application.port;

import com.altbank.cardsapi.domain.model.VirtualCard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VirtualCardRepository {
    Optional<VirtualCard> findById(UUID cardId);
    Optional<VirtualCard> findActiveByAccountId(UUID accountId);
    Optional<VirtualCard> findByProcessorCardId(String processorCardId);
    List<VirtualCard> listAll();
    List<VirtualCard> listByAccountId(UUID accountId);
    void persist(VirtualCard virtualCard);
}
