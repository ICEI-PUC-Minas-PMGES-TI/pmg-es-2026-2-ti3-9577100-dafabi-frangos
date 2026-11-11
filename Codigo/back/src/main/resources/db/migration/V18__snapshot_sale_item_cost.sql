ALTER TABLE sale_item
    ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(14, 2);

UPDATE sale_item si
SET unit_cost = p.current_cost
FROM product p
WHERE si.product_id = p.id
  AND si.unit_cost IS NULL;

UPDATE sale_item
SET unit_cost = 0
WHERE unit_cost IS NULL;

ALTER TABLE sale_item
    ALTER COLUMN unit_cost SET DEFAULT 0,
    ALTER COLUMN unit_cost SET NOT NULL;
