CREATE TABLE integration_import_batch (
    id UUID PRIMARY KEY,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('IFOOD', 'FOOD_99')),
    report_type VARCHAR(20) NOT NULL CHECK (report_type IN ('ORDERS', 'ITEMS')),
    file_name VARCHAR(255) NOT NULL,
    file_sha256 VARCHAR(64) NOT NULL,
    period_start DATE,
    period_end DATE,
    status VARCHAR(24) NOT NULL CHECK (status IN ('READY', 'IMPORTED', 'IMPORTED_WITH_ERRORS')),
    row_count INTEGER NOT NULL DEFAULT 0,
    valid_row_count INTEGER NOT NULL DEFAULT 0,
    invalid_row_count INTEGER NOT NULL DEFAULT 0,
    duplicate_row_count INTEGER NOT NULL DEFAULT 0,
    summary_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    imported_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_integration_import_file UNIQUE (provider, file_sha256)
);

CREATE INDEX ix_integration_import_created_at ON integration_import_batch(created_at DESC);

CREATE TABLE integration_import_row (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES integration_import_batch(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,
    row_key VARCHAR(255),
    valid BOOLEAN NOT NULL,
    duplicate_in_file BOOLEAN NOT NULL DEFAULT FALSE,
    duplicate_in_system BOOLEAN NOT NULL DEFAULT FALSE,
    issues_json TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    CONSTRAINT uk_integration_import_row UNIQUE (batch_id, row_number)
);

CREATE TABLE marketplace_order (
    id UUID PRIMARY KEY,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('IFOOD', 'FOOD_99')),
    external_store_id VARCHAR(120) NOT NULL,
    store_name VARCHAR(255),
    external_order_id VARCHAR(160) NOT NULL,
    order_at TIMESTAMP WITH TIME ZONE NOT NULL,
    raw_status VARCHAR(100) NOT NULL,
    normalized_status VARCHAR(32) NOT NULL,
    shift_name VARCHAR(80),
    items_value NUMERIC(14,2),
    customer_paid NUMERIC(14,2),
    customer_delivery_fee NUMERIC(14,2),
    ifood_promotion NUMERIC(14,2),
    store_promotion NUMERIC(14,2),
    network_promotion NUMERIC(14,2),
    service_fee NUMERIC(14,2),
    fees_and_commissions NUMERIC(14,2),
    net_reported NUMERIC(14,2),
    payment_method_raw VARCHAR(160),
    delivery_type VARCHAR(100),
    logistics_product VARCHAR(160),
    sales_channel VARCHAR(100),
    source_batch_id UUID NOT NULL REFERENCES integration_import_batch(id),
    source_payload_json TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_marketplace_order_external UNIQUE (provider, external_store_id, external_order_id)
);

CREATE INDEX ix_marketplace_order_date ON marketplace_order(order_at DESC);
CREATE INDEX ix_marketplace_order_provider_status ON marketplace_order(provider, normalized_status);

CREATE TABLE marketplace_item_snapshot (
    id UUID PRIMARY KEY,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('IFOOD', 'FOOD_99')),
    external_store_id VARCHAR(120) NOT NULL,
    store_name VARCHAR(255),
    business_date DATE NOT NULL,
    external_item_name VARCHAR(255) NOT NULL,
    normalized_item_name VARCHAR(255) NOT NULL,
    item_revenue NUMERIC(14,2) NOT NULL,
    sold_quantity NUMERIC(14,3) NOT NULL,
    source_batch_id UUID NOT NULL REFERENCES integration_import_batch(id),
    source_payload_json TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_marketplace_item_snapshot UNIQUE (provider, external_store_id, business_date, external_item_name)
);

CREATE INDEX ix_marketplace_item_date ON marketplace_item_snapshot(provider, business_date);

