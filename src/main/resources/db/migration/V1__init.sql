CREATE TABLE customers (
  id CHAR(36) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  document VARCHAR(32) NOT NULL,
  email VARCHAR(120) NOT NULL,
  phone VARCHAR(40) NULL,
  address_street VARCHAR(160) NOT NULL,
  address_number VARCHAR(40) NOT NULL,
  address_city VARCHAR(80) NOT NULL,
  address_state VARCHAR(80) NOT NULL,
  address_zip_code VARCHAR(24) NOT NULL,
  address_country VARCHAR(80) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_customers_document (document)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE accounts (
  id CHAR(36) NOT NULL,
  customer_id CHAR(36) NOT NULL,
  status VARCHAR(24) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  cancelled_at TIMESTAMP(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_accounts_customer_id (customer_id),
  KEY idx_accounts_status (status),
  CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cards (
  id CHAR(36) NOT NULL,
  account_id CHAR(36) NOT NULL,
  card_type VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  deactivated_at TIMESTAMP(6) NULL,
  PRIMARY KEY (id),
  KEY idx_cards_account_status (account_id, status),
  KEY idx_cards_type (card_type),
  CONSTRAINT fk_cards_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE physical_cards (
  card_id CHAR(36) NOT NULL,
  tracking_id VARCHAR(64) NOT NULL,
  delivery_status VARCHAR(24) NOT NULL,
  delivery_date TIMESTAMP(6) NULL,
  delivery_return_reason VARCHAR(255) NULL,
  delivery_address VARCHAR(255) NULL,
  delivered_at TIMESTAMP(6) NULL,
  validated_at TIMESTAMP(6) NULL,
  reissue_reason VARCHAR(24) NULL,
  previous_physical_card_id CHAR(36) NULL,
  PRIMARY KEY (card_id),
  UNIQUE KEY uk_physical_cards_tracking_id (tracking_id),
  KEY idx_physical_cards_delivery_status (delivery_status),
  CONSTRAINT fk_physical_cards_card FOREIGN KEY (card_id) REFERENCES cards (id),
  CONSTRAINT fk_physical_cards_previous FOREIGN KEY (previous_physical_card_id) REFERENCES physical_cards (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE virtual_cards (
  card_id CHAR(36) NOT NULL,
  processor_account_id VARCHAR(64) NOT NULL,
  processor_card_id VARCHAR(64) NOT NULL,
  cvv_expiration_at TIMESTAMP(6) NULL,
  PRIMARY KEY (card_id),
  UNIQUE KEY uk_virtual_cards_processor_card_id (processor_card_id),
  KEY idx_virtual_cards_processor_account_id (processor_account_id),
  CONSTRAINT fk_virtual_cards_card FOREIGN KEY (card_id) REFERENCES cards (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
