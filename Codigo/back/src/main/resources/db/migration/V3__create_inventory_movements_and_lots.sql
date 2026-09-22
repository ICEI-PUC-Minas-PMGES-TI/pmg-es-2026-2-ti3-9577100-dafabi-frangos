-- V3: Histórico de movimentações de estoque e lotes de produtos perecíveis.

CREATE TABLE IF NOT EXISTS inventory_lot (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    batch VARCHAR(100) NOT NULL,
    received_at DATE NOT NULL,
    expiry DATE NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_inventory_lot_product
        FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT ck_inventory_lot_expiry_after_receipt
        CHECK (expiry >= received_at),
    CONSTRAINT uk_inventory_lot_product_batch UNIQUE (product_id, batch)
);

CREATE TABLE IF NOT EXISTS stock_movement (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    lot_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    movement_type VARCHAR(40) NOT NULL,
    previous_quantity INTEGER NOT NULL CHECK (previous_quantity >= 0),
    new_quantity INTEGER NOT NULL CHECK (new_quantity >= 0),
    quantity_delta INTEGER NOT NULL,
    origin VARCHAR(100) NOT NULL,
    reason VARCHAR(500),
    performed_by VARCHAR(100) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    CONSTRAINT fk_stock_movement_product
        FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_stock_movement_lot
        FOREIGN KEY (lot_id) REFERENCES inventory_lot(id),
    CONSTRAINT ck_stock_movement_quantity_delta
        CHECK (quantity_delta = new_quantity - previous_quantity)
);

CREATE INDEX IF NOT EXISTS ix_stock_movement_product_occurred_at
    ON stock_movement (product_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_inventory_lot_product_expiry
    ON inventory_lot (product_id, expiry);
