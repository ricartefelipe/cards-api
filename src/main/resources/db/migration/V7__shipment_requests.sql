-- Remessa como intencao duravel: a chamada a transportadora acontece fora da transacao e fora
-- de locks, e o cartao so recebe tracking quando a resposta chega. Ate la nao ha tracking.
ALTER TABLE physical_cards MODIFY tracking_id VARCHAR(64) NULL;

CREATE TABLE shipment_requests (
  id CHAR(36) NOT NULL,
  physical_card_id CHAR(36) NOT NULL,
  delivery_address VARCHAR(255) NOT NULL,
  status VARCHAR(16) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  tracking_id VARCHAR(64) NULL,
  last_error VARCHAR(255) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_shipment_requests_card (physical_card_id),
  KEY idx_shipment_requests_status (status, updated_at),
  CONSTRAINT fk_shipment_requests_card FOREIGN KEY (physical_card_id) REFERENCES physical_cards (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
