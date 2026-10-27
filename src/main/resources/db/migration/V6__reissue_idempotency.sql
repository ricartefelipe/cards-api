-- Idempotencia de reemissao: cada cartao so pode ser predecessor de UM novo cartao.
-- Impede dois cartoes remotos a partir da mesma reemissao sob concorrencia (barreira de
-- integridade; retry sequencial ja e barrado pelo estado do cartao antigo = inativo).
-- UNIQUE sobre coluna anulavel aceita multiplos NULL (cartoes originais, sem predecessor).

-- Virtual: troca o indice nao-unico pelo unico (o FK passa a usar o indice unico).
ALTER TABLE virtual_cards ADD CONSTRAINT uk_virtual_cards_previous UNIQUE (previous_virtual_card_id);
ALTER TABLE virtual_cards DROP INDEX idx_virtual_cards_previous;

-- Fisico: adiciona o unico (o FK ja existente passa a usa-lo).
ALTER TABLE physical_cards ADD CONSTRAINT uk_physical_cards_previous UNIQUE (previous_physical_card_id);
