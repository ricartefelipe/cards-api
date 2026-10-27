package com.altbank.cardsapi.infrastructure.persistence;

import com.altbank.cardsapi.application.port.WebhookEventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.Objects;

@ApplicationScoped
public class JpaWebhookEventRepository implements WebhookEventRepository {

    private final EntityManager entityManager;
    private final Clock clock;

    @Inject
    public JpaWebhookEventRepository(EntityManager entityManager, Clock clock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public boolean recordIfAbsent(String fingerprint, String eventType) {
        Long existing = entityManager.createQuery(
                        "SELECT COUNT(e) FROM WebhookEvent e WHERE e.fingerprint = :fp", Long.class)
                .setParameter("fp", fingerprint)
                .getSingleResult();
        if (existing != null && existing > 0) {
            return false;
        }
        // A UNIQUE no banco e a barreira final sob concorrencia; o commit falha para o
        // perdedor da corrida (sua transacao, e o efeito, sao revertidos).
        entityManager.persist(new WebhookEvent(fingerprint, eventType, clock));
        return true;
    }
}
