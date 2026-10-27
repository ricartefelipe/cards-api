package com.altbank.cardsapi.application.port;

public interface WebhookEventRepository {

    /**
     * Registra o evento se o fingerprint ainda nao foi visto.
     *
     * @return {@code true} se era novo (deve processar o efeito);
     *         {@code false} se ja processado (duplicata — resposta idempotente).
     */
    boolean recordIfAbsent(String fingerprint, String eventType);
}
