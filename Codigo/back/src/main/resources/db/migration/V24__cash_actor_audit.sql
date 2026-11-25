-- Older records have no reliable actor; do not invent one.
ALTER TABLE cash_register ADD COLUMN closed_by_id UUID;
ALTER TABLE cash_register ADD COLUMN closed_by_name VARCHAR(150);
ALTER TABLE cash_movement ADD COLUMN performed_by_id UUID;
ALTER TABLE cash_movement ADD COLUMN performed_by_name VARCHAR(150);
ALTER TABLE sale ADD COLUMN operator_name VARCHAR(150);
