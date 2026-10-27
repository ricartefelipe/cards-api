package com.altbank.cardsapi.application.port;

public interface AuditRepository {

    /**
     * Persiste um registro de auditoria. Deve participar da transacao da acao auditada.
     * Nao recebe material sensivel.
     */
    void record(String correlationId, String actorSubject, String action, String targetType, String targetId);
}
