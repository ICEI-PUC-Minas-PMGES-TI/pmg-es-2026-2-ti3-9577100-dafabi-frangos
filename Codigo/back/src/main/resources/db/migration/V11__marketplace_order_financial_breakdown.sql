ALTER TABLE marketplace_order
    ADD COLUMN marketing_cost NUMERIC(14,2),
    ADD COLUMN logistics_cost NUMERIC(14,2),
    ADD COLUMN refund_amount NUMERIC(14,2),
    ADD COLUMN free_delivery_cost NUMERIC(14,2),
    ADD COLUMN tip_amount NUMERIC(14,2),
    ADD COLUMN original_item_count NUMERIC(14,3),
    ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN cancellation_responsible VARCHAR(180),
    ADD COLUMN cancellation_reason VARCHAR(500);
