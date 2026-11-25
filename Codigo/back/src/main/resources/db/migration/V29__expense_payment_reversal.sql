-- Estorno do pagamento de despesa: novo status e registro de quando e por que foi estornado.
ALTER TABLE expense DROP CONSTRAINT IF EXISTS ck_expense_status;
ALTER TABLE expense ADD CONSTRAINT ck_expense_status CHECK (status IN ('PENDING', 'CONSOLIDATED', 'CANCELLED', 'REVERSED'));
ALTER TABLE expense ADD COLUMN reversed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE expense ADD COLUMN reversal_reason VARCHAR(500);
