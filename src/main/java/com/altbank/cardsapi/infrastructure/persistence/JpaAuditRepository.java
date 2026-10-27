package com.altbank.cardsapi.infrastructure.persistence;

import com.altbank.cardsapi.application.port.AuditRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.Objects;

@ApplicationScoped
public class JpaAuditRepository implements AuditRepository {

    private final EntityManager entityManager;
    private final Clock clock;

    @Inject
    public JpaAuditRepository(EntityManager entityManager, Clock clock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void record(String correlationId, String actorSubject, String action, String targetType, String targetId) {
        // Participa da transacao ja ativa na acao auditada (mesma unidade de trabalho).
        entityManager.persist(new AuditLog(correlationId, actorSubject, action, targetType, targetId, clock));
    }
}
