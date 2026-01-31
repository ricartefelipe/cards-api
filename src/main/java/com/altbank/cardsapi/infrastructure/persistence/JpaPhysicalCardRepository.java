package com.altbank.cardsapi.infrastructure.persistence;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.port.PhysicalCardRepository;
import com.altbank.cardsapi.domain.model.CardStatus;
import com.altbank.cardsapi.domain.model.PhysicalCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaPhysicalCardRepository implements PhysicalCardRepository {

    private final EntityManager entityManager;

    @Inject

    public JpaPhysicalCardRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<PhysicalCard> findById(UUID cardId) {
        return Optional.ofNullable(entityManager.find(PhysicalCard.class, cardId));
    }

    @Override
    public Optional<PhysicalCard> findByTrackingId(String trackingId) {
        return entityManager.createQuery("SELECT p FROM PhysicalCard p WHERE p.trackingId = :trackingId", PhysicalCard.class)
                .setParameter("trackingId", trackingId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<PhysicalCard> findActiveByAccountId(UUID accountId) {
        return entityManager.createQuery(
                        "SELECT p FROM PhysicalCard p WHERE p.account.id = :accountId AND p.status = :status ORDER BY p.createdAt DESC",
                        PhysicalCard.class
                )
                .setParameter("accountId", accountId)
                .setParameter("status", CardStatus.ACTIVE)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    @Override
    public void persist(PhysicalCard physicalCard) {
        entityManager.persist(physicalCard);
    }
}
