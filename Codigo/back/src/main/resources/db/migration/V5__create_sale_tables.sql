-- V5: Criação das tabelas de Venda (sale) e Itens da Venda (sale_item)

CREATE TABLE IF NOT EXISTS sale (

    id UUID PRIMARY KEY,

    cash_register_id UUID,

    operator_id UUID NOT NULL,

    sale_number BIGINT NOT NULL,

    date_time TIMESTAMP WITH TIME ZONE NOT NULL,

    origin VARCHAR(20) NOT NULL,

    payment_method VARCHAR(20) NOT NULL,

    subtotal NUMERIC(14,2) NOT NULL,

    total NUMERIC(14,2) NOT NULL,

    status VARCHAR(20) NOT NULL,

    CONSTRAINT fk_sale_cash_register
        FOREIGN KEY (cash_register_id) REFERENCES cash_register(id),

    CONSTRAINT ck_sale_origin
        CHECK (origin IN ('COUNTER', 'IFOOD', 'FOOD_99')),

    CONSTRAINT ck_sale_payment_method
        CHECK (payment_method IN ('CASH', 'PIX', 'DEBIT', 'CREDIT')),

    CONSTRAINT ck_sale_status
        CHECK (status IN ('COMPLETED', 'CANCELLED', 'REFUNDED')),

    CONSTRAINT ck_sale_cash_register_counter
        CHECK (
            origin <> 'COUNTER'
            OR cash_register_id IS NOT NULL
        )
);

CREATE TABLE IF NOT EXISTS sale_item (

    id UUID PRIMARY KEY,

    sale_id UUID NOT NULL,

    product_id UUID NOT NULL,

    quantity NUMERIC(14,3) NOT NULL,

    unit_price NUMERIC(14,2) NOT NULL,

    subtotal NUMERIC(14,2) NOT NULL,

    CONSTRAINT fk_sale_item_sale
        FOREIGN KEY (sale_id) REFERENCES sale(id) ON DELETE CASCADE,

    CONSTRAINT fk_sale_item_product
        FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX IF NOT EXISTS ix_sale_cash_register
    ON sale (cash_register_id);

CREATE INDEX IF NOT EXISTS ix_sale_operator
    ON sale (operator_id);

CREATE INDEX IF NOT EXISTS ix_sale_date_time
    ON sale (date_time);

CREATE INDEX IF NOT EXISTS ix_sale_item_sale
    ON sale_item (sale_id);

CREATE INDEX IF NOT EXISTS ix_sale_item_product
    ON sale_item (product_id);