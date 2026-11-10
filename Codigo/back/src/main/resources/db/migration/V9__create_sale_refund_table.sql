-- V9: Criação da tabela de Estorno de Venda (sale_refund)
-- Cada venda pode ter no máximo um estorno (uk_sale_refund_sale), o que impede
-- reversões duplicadas de caixa e estoque mesmo sob acesso concorrente.

CREATE TABLE IF NOT EXISTS sale_refund (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    cash_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    cash_register_id UUID,
    reason VARCHAR(500) NOT NULL,
    refunded_by_id UUID NOT NULL,
    refunded_by_name VARCHAR(150) NOT NULL,
    idempotency_key VARCHAR(100),
    refunded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_sale_refund_sale
        FOREIGN KEY (sale_id) REFERENCES sale(id),
    CONSTRAINT fk_sale_refund_cash_register
        FOREIGN KEY (cash_register_id) REFERENCES cash_register(id),
    CONSTRAINT uk_sale_refund_sale UNIQUE (sale_id),
    CONSTRAINT uk_sale_refund_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_sale_refund_amount_non_negative
        CHECK (amount >= 0),
    CONSTRAINT ck_sale_refund_cash_amount_non_negative
        CHECK (cash_amount >= 0)
);

CREATE INDEX IF NOT EXISTS ix_sale_refund_refunded_at
    ON sale_refund (refunded_at DESC);
