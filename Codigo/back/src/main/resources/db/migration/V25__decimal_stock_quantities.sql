-- Permite quantidades fracionadas no estoque (ex.: "meio frango" = 0,5 unidade),
-- usadas pela baixa integrada de marketplaces. Valores inteiros existentes são preservados.
ALTER TABLE product_stock
    ALTER COLUMN quantity TYPE NUMERIC(14,3) USING quantity::NUMERIC(14,3);

ALTER TABLE inventory_lot
    ALTER COLUMN quantity TYPE NUMERIC(14,3) USING quantity::NUMERIC(14,3);

ALTER TABLE stock_movement
    ALTER COLUMN previous_quantity TYPE NUMERIC(14,3) USING previous_quantity::NUMERIC(14,3),
    ALTER COLUMN new_quantity TYPE NUMERIC(14,3) USING new_quantity::NUMERIC(14,3),
    ALTER COLUMN quantity_delta TYPE NUMERIC(14,3) USING quantity_delta::NUMERIC(14,3);
