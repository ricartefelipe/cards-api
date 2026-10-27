-- Deduplicacao duravel de eventos de webhook (idempotencia que sobrevive a restart
-- e a multiplas instancias). O fingerprint identifica o evento sem material sensivel
-- (hash; nao inclui CVV). A UNIQUE e a barreira de integridade sob concorrencia.
CREATE TABLE webhook_events (
  id CHAR(36) NOT NULL,
  fingerprint VARCHAR(128) NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  received_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_webhook_events_fingerprint (fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
