-- Guarda o valor recebido em dinheiro para mostrar o troco no detalhe da venda.
ALTER TABLE sale ADD COLUMN amount_received NUMERIC(14,2);
