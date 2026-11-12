ALTER TABLE purchase
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30),
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE purchase
    ADD CONSTRAINT ck_purchase_payment_status
        CHECK (payment_status IN ('PENDING', 'PAID'));

ALTER TABLE purchase
    ADD CONSTRAINT ck_purchase_payment_details
        CHECK ((payment_status = 'PENDING' AND payment_method IS NULL AND paid_at IS NULL)
            OR (payment_status = 'PAID' AND payment_method IS NOT NULL AND paid_at IS NOT NULL));

ALTER TABLE purchase_item
    ADD COLUMN IF NOT EXISTS previous_product_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS resulting_product_cost NUMERIC(14, 2) NOT NULL DEFAULT 0;

UPDATE purchase_item pi
SET previous_product_cost = p.current_cost,
    resulting_product_cost = p.current_cost
FROM product p
WHERE pi.product_id = p.id;
