package com.altbank.cardsapi.application.port;

import com.altbank.cardsapi.domain.model.ShipmentRequest;
import com.altbank.cardsapi.domain.model.ShipmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRequestRepository {

    Optional<ShipmentRequest> findById(UUID id);

    /** Carrega com bloqueio de escrita: quem reivindica o envio precisa ser o unico a ve-lo PENDING. */
    Optional<ShipmentRequest> findForUpdate(UUID id);

    Optional<ShipmentRequest> findByPhysicalCardId(UUID physicalCardId);

    List<ShipmentRequest> findByStatus(ShipmentStatus status, int limit);

    List<ShipmentRequest> findInProgressUpdatedBefore(LocalDateTime instant, int limit);

    void persist(ShipmentRequest request);
}
