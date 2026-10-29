package com.altbank.cardsapi.infrastructure.persistence;

import jakarta.inject.Inject;
import com.altbank.cardsapi.application.exception.ConflictException;
import com.altbank.cardsapi.application.exception.ErrorCode;
import com.altbank.cardsapi.application.port.VirtualCardRepository;
import com.altbank.cardsapi.domain.model.CardStatus;
import com.altbank.cardsapi.domain.model.VirtualCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaVirtualCardRepository implements VirtualCardRepository {

    /** Restricao que deixa um cartao ser predecessor de um unico outro (V6__reissue_idempotency). */
    private static final String UNIQUE_PREVIOUS = "uk_virtual_cards_previous";

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
    public List<VirtualCard> listAll(int offset, int limit) {
        return entityManager.createQuery(
                        "SELECT v FROM VirtualCard v JOIN FETCH v.account ORDER BY v.createdAt DESC",
                        VirtualCard.class
                )
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<VirtualCard> listByAccountId(UUID accountId) {
        return entityManager.createQuery(
                        "SELECT v FROM VirtualCard v JOIN FETCH v.account WHERE v.account.id = :accountId ORDER BY v.createdAt DESC",
                        VirtualCard.class
                )
                .setParameter("accountId", accountId)
                .getResultList();
    }

    @Override
    public void persist(VirtualCard virtualCard) {
        entityManager.persist(virtualCard);
        UniqueConflicts.flushTranslating(
                entityManager,
                UNIQUE_PREVIOUS,
                () -> new ConflictException(
                        ErrorCode.VIRTUAL_CARD_ALREADY_REISSUED,
                        "Virtual card has already been reissued"));
    }
}
