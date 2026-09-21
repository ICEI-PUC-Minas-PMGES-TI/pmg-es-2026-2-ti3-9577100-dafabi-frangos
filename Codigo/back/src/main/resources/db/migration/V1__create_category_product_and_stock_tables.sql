-- V1: Criação das tabelas de Categoria, Produto e Estoque

CREATE TABLE IF NOT EXISTS category (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS product (
    id UUID PRIMARY KEY,
    category_id UUID,
    name VARCHAR(255) NOT NULL,
    sale_price NUMERIC(14,2) NOT NULL,
    current_cost NUMERIC(14,2) NOT NULL,
    unit VARCHAR(10) NOT NULL DEFAULT 'UN',
    barcode VARCHAR(64),
    perishable BOOLEAN NOT NULL DEFAULT false,
    frequent BOOLEAN NOT NULL DEFAULT false,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_barcode ON product (barcode) 
WHERE barcode IS NOT NULL AND barcode <> '';

CREATE TABLE IF NOT EXISTS product_stock (
    product_id UUID PRIMARY KEY,
    quantity INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_stock_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);
