CREATE TABLE store_settings (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    address VARCHAR(250) NOT NULL,
    default_payment VARCHAR(20) NOT NULL,
    compact_mode BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_store_settings_default_payment
        CHECK (default_payment IN ('CASH', 'PIX', 'DEBIT', 'CREDIT'))
);

INSERT INTO store_settings (id, name, phone, address, default_payment, compact_mode, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'DaFabi Frangos', '(31) 99123-4567',
        'Rua das Palmeiras, 125 — Belo Horizonte/MG', 'PIX', FALSE, CURRENT_TIMESTAMP);
