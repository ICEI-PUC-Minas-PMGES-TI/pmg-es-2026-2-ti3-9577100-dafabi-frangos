ALTER TABLE inventory_lot
    DROP CONSTRAINT IF EXISTS inventory_lot_quantity_check;

ALTER TABLE inventory_lot
    ADD CONSTRAINT ck_inventory_lot_quantity_non_negative CHECK (quantity >= 0);
