-- V8: Criação das tabelas de Compra (purchase) e Itens da Compra (purchase_item)

CREATE TABLE IF NOT EXISTS purchase (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL,
    purchase_date DATE NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    created_by VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchase_supplier
        FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    CONSTRAINT ck_purchase_total_amount_non_negative
        CHECK (total_amount >= 0),
    CONSTRAINT ck_purchase_status
        CHECK (status IN ('REGISTERED', 'CANCELED'))
);

CREATE TABLE IF NOT EXISTS purchase_item (
    id UUID PRIMARY KEY,
    purchase_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_cost NUMERIC(14,2) NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    CONSTRAINT fk_purchase_item_purchase
        FOREIGN KEY (purchase_id) REFERENCES purchase(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_item_product
        FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT ck_purchase_item_quantity_positive
        CHECK (quantity > 0),
    CONSTRAINT ck_purchase_item_unit_cost_non_negative
        CHECK (unit_cost >= 0),
    CONSTRAINT ck_purchase_item_total_amount_non_negative
        CHECK (total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS ix_purchase_supplier
    ON purchase (supplier_id);

CREATE INDEX IF NOT EXISTS ix_purchase_date
    ON purchase (purchase_date DESC);

CREATE INDEX IF NOT EXISTS ix_purchase_status
    ON purchase (status);

CREATE INDEX IF NOT EXISTS ix_purchase_item_purchase
    ON purchase_item (purchase_id);

CREATE INDEX IF NOT EXISTS ix_purchase_item_product
    ON purchase_item (product_id);
