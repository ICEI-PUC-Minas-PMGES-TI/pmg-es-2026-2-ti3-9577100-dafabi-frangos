-- V6: Criação da tabela de Fornecedores (supplier)

CREATE TABLE IF NOT EXISTS supplier (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(150) NOT NULL,
    notes VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_supplier_store FOREIGN KEY (store_id) REFERENCES store(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_store_email
    ON supplier (store_id, lower(email));

CREATE INDEX IF NOT EXISTS ix_supplier_store_name
    ON supplier (store_id, name);
