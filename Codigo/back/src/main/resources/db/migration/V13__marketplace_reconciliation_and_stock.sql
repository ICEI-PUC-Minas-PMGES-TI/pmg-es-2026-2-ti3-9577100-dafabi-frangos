CREATE TABLE marketplace_product_mapping (
    id UUID PRIMARY KEY,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('IFOOD', 'FOOD_99')),
    external_store_id VARCHAR(120) NOT NULL,
    external_item_name VARCHAR(255) NOT NULL,
    product_id UUID REFERENCES product(id),
    quantity_per_item NUMERIC(12,3),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_marketplace_product_mapping_component CHECK
        ((product_id IS NULL AND quantity_per_item IS NULL) OR
         (product_id IS NOT NULL AND quantity_per_item > 0)),
    CONSTRAINT uk_marketplace_product_mapping UNIQUE
        (provider, external_store_id, external_item_name, product_id)
);

CREATE TABLE marketplace_stock_baseline (
    id UUID PRIMARY KEY,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('IFOOD', 'FOOD_99')),
    external_store_id VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_marketplace_stock_baseline UNIQUE (provider, external_store_id)
);

CREATE TABLE marketplace_stock_application (
    id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL REFERENCES marketplace_item_snapshot(id),
    product_id UUID NOT NULL REFERENCES product(id),
    -- Negative quantities can represent corrections/returns in an item-by-day export.
    reported_quantity NUMERIC(14,3) NOT NULL,
    applied_quantity NUMERIC(14,3) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'APPLIED', 'INSUFFICIENT_STOCK', 'RETURN_REVIEW', 'BEFORE_BASELINE')),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_marketplace_stock_application UNIQUE (snapshot_id, product_id)
);

CREATE INDEX ix_marketplace_mapping_source
    ON marketplace_product_mapping(provider, external_store_id, external_item_name);
CREATE INDEX ix_marketplace_stock_baseline_source
    ON marketplace_stock_baseline(provider, external_store_id, start_date);
CREATE INDEX ix_marketplace_stock_application_status
    ON marketplace_stock_application(status, updated_at DESC);

CREATE TABLE marketplace_settlement (
    id UUID PRIMARY KEY,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('IFOOD', 'FOOD_99')),
    external_store_id VARCHAR(120) NOT NULL,
    external_settlement_id VARCHAR(160) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    gross_amount NUMERIC(14,2),
    fees_amount NUMERIC(14,2),
    adjustments_amount NUMERIC(14,2),
    retained_amount NUMERIC(14,2),
    amount_received NUMERIC(14,2),
    payout_date DATE,
    status VARCHAR(40) NOT NULL,
    source_batch_id UUID REFERENCES integration_import_batch(id),
    source_reference TEXT,
    source_payload_json TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_marketplace_settlement_external UNIQUE
        (provider, external_store_id, external_settlement_id)
);

CREATE INDEX ix_marketplace_settlement_period
    ON marketplace_settlement(provider, period_start, period_end);
