package com.altbank.cardsapi.infrastructure.persistence;

import com.altbank.cardsapi.application.port.ShipmentRequestRepository;
import com.altbank.cardsapi.domain.model.ShipmentRequest;
import com.altbank.cardsapi.domain.model.ShipmentStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaShipmentRequestRepository implements ShipmentRequestRepository {

    private final EntityManager entityManager;

    @Inject
    public JpaShipmentRequestRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<ShipmentRequest> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(ShipmentRequest.class, id.toString()));
    }

    @Override
    public Optional<ShipmentRequest> findForUpdate(UUID id) {
        return Optional.ofNullable(entityManager.find(ShipmentRequest.class, id.toString(), LockModeType.PESSIMISTIC_WRITE));
    }

    @Override
    public Optional<ShipmentRequest> findByPhysicalCardId(UUID physicalCardId) {
        return entityManager.createQuery(
                        "SELECT s FROM ShipmentRequest s WHERE s.physicalCardId = :cardId", ShipmentRequest.class)
                .setParameter("cardId", physicalCardId.toString())
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<ShipmentRequest> findByStatus(ShipmentStatus status, int limit) {
        return entityManager.createQuery(
                        "SELECT s FROM ShipmentRequest s WHERE s.status = :status ORDER BY s.createdAt", ShipmentRequest.class)
                .setParameter("status", status)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<ShipmentRequest> findInProgressUpdatedBefore(LocalDateTime instant, int limit) {
        return entityManager.createQuery(
                        "SELECT s FROM ShipmentRequest s WHERE s.status = :status AND s.updatedAt < :instant ORDER BY s.updatedAt",
                        ShipmentRequest.class)
                .setParameter("status", ShipmentStatus.IN_PROGRESS)
                .setParameter("instant", instant)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public void persist(ShipmentRequest request) {
        entityManager.persist(request);
    }
}
