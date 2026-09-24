-- V4: Criação das tabelas de Loja (store), Caixa (cash_register) e Movimentações de Caixa (cash_movement)

CREATE TABLE IF NOT EXISTS store (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    address VARCHAR(255),
    default_payment_method VARCHAR(30) DEFAULT 'CASH',
    compact_mode BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inserção da loja inicial padrão para operação em loja única
INSERT INTO store (id, name, phone, address, default_payment_method, compact_mode)
VALUES ('00000000-0000-0000-0000-000000000001', 'DaFabi Frangos', '(31) 98270-7399', 'Av. Ver. Cícero Ildefonso, 615 - João Pinheiro, Belo Horizonte - MG, 30530-000', 'CASH', false)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS cash_register (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    operator_id UUID,
    operator_name VARCHAR(150) NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    initial_balance NUMERIC(14,2) NOT NULL,
    expected_balance_at_close NUMERIC(14,2),
    counted_balance NUMERIC(14,2),
    difference NUMERIC(14,2),
    justification VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cash_register_store FOREIGN KEY (store_id) REFERENCES store(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_open_cash_per_store
    ON cash_register (store_id)
    WHERE status = 'OPEN';

CREATE INDEX IF NOT EXISTS ix_cash_register_store_opened_at
    ON cash_register (store_id, opened_at DESC);

CREATE TABLE IF NOT EXISTS cash_movement (
    id UUID PRIMARY KEY,
    cash_register_id UUID NOT NULL,
    direction VARCHAR(10) NOT NULL,
    movement_type VARCHAR(40) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    origin VARCHAR(100),
    reference_type VARCHAR(50),
    reference_id UUID,
    description VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_cash_movement_cash_register
        FOREIGN KEY (cash_register_id) REFERENCES cash_register(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_cash_movement_register_occurred_at
    ON cash_movement (cash_register_id, occurred_at DESC);
