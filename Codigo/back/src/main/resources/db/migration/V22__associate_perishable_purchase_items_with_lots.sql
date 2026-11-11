ALTER TABLE purchase_item
    ADD COLUMN IF NOT EXISTS batch VARCHAR(100),
    ADD COLUMN IF NOT EXISTS expiry DATE;

ALTER TABLE purchase_item
    ADD CONSTRAINT ck_purchase_item_batch_expiry_pair
        CHECK ((batch IS NULL AND expiry IS NULL) OR (batch IS NOT NULL AND expiry IS NOT NULL));
