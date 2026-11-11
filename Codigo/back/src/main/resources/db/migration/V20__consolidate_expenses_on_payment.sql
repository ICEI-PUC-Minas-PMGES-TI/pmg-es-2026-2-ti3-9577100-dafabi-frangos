ALTER TABLE expense
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP WITH TIME ZONE;

-- Despesas antigas em dinheiro já criaram uma saída de caixa no cadastro; preserva-se
-- esse registro como pago em vez de reclassificá-lo como pendente sem movimento.
UPDATE expense e
SET status = 'CONSOLIDATED',
    paid_at = COALESCE(e.paid_at, e.updated_at)
WHERE e.status = 'PENDING'
  AND EXISTS (
      SELECT 1 FROM cash_movement cm
      WHERE cm.reference_type = 'EXPENSE' AND cm.reference_id = e.id
  );

UPDATE expense
SET paid_at = COALESCE(paid_at, updated_at)
WHERE status = 'CONSOLIDATED';
