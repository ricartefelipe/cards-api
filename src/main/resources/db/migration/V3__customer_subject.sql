-- Vinculo do titular com a identidade do IdP (claim sub). NULL permitido para
-- clientes ainda nao vinculados; UNIQUE sobre coluna anulavel aceita multiplos NULL
-- (um subject pertence a no maximo um cliente). Compativel com dados existentes.
ALTER TABLE customers ADD COLUMN subject VARCHAR(64) NULL;
ALTER TABLE customers ADD CONSTRAINT uk_customers_subject UNIQUE (subject);
