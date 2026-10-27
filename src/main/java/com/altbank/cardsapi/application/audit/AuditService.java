package com.altbank.cardsapi.application.audit;

import com.altbank.cardsapi.application.port.AuditRepository;
import com.altbank.cardsapi.application.security.CallerContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.UUID;

/**
 * Registra acoes administrativas na trilha de auditoria, dentro da transacao da acao.
 * O ator vem da identidade verificada; nunca inclui material sensivel (CVV/PAN/segredos).
 */
@ApplicationScoped
public class AuditService {

    private final AuditRepository auditRepository;
    private final CallerContext caller;

    @Inject
    public AuditService(AuditRepository auditRepository, CallerContext caller) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.caller = Objects.requireNonNull(caller, "caller");
    }

    public void record(AuditAction action, String targetType, UUID targetId) {
        Objects.requireNonNull(action, "action");
        auditRepository.record(
                UUID.randomUUID().toString(),
                caller.subject(),
                action.name(),
                targetType,
                targetId == null ? null : targetId.toString());
    }
}
