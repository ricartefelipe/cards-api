package com.altbank.cardsapi.infrastructure.persistence;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.CardStatus;
import com.altbank.cardsapi.domain.model.VirtualCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaVirtualCardRepository implements VirtualCardRepository {

    private final EntityManager entityManager;

    @Inject

    public JpaVirtualCardRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<VirtualCard> findById(UUID cardId) {
        return Optional.ofNullable(entityManager.find(VirtualCard.class, cardId));
    }

    @Override
    public Optional<VirtualCard> findActiveByAccountId(UUID accountId) {
        return entityManager.createQuery(
                        "SELECT v FROM VirtualCard v WHERE v.account.id = :accountId AND v.status = :status ORDER BY v.createdAt DESC",
                        VirtualCard.class
                )
                .setParameter("accountId", accountId)
                .setParameter("status", CardStatus.ACTIVE)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<VirtualCard> findByProcessorCardId(String processorCardId) {
        return entityManager.createQuery("SELECT v FROM VirtualCard v WHERE v.processorCardId = :processorCardId", VirtualCard.class)
                .setParameter("processorCardId", processorCardId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public void persist(VirtualCard virtualCard) {
        entityManager.persist(virtualCard);
    }
}
