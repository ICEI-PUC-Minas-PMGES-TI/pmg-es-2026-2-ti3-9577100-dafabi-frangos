-- Evita venda duplicada quando a mesma tentativa é reenviada pelo PDV.
ALTER TABLE sale ADD COLUMN idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX uk_sale_idempotency_key ON sale (idempotency_key) WHERE idempotency_key IS NOT NULL;
