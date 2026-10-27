-- Trilha de auditoria de acoes administrativas. Sem material sensivel (sem CVV/PAN/segredos).
CREATE TABLE audit_logs (
  id CHAR(36) NOT NULL,
  correlation_id VARCHAR(36) NOT NULL,
  actor_subject VARCHAR(64) NULL,
  action VARCHAR(48) NOT NULL,
  target_type VARCHAR(32) NULL,
  target_id VARCHAR(36) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_audit_logs_action (action),
  KEY idx_audit_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
