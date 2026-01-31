ALTER TABLE virtual_cards ADD COLUMN reissue_reason VARCHAR(24) NULL;
ALTER TABLE virtual_cards ADD COLUMN previous_virtual_card_id CHAR(36) NULL;
ALTER TABLE virtual_cards ADD KEY idx_virtual_cards_previous (previous_virtual_card_id);
ALTER TABLE virtual_cards ADD CONSTRAINT fk_virtual_cards_previous FOREIGN KEY (previous_virtual_card_id) REFERENCES virtual_cards (card_id);
