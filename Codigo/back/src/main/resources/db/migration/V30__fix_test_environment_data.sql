-- Correções de dados do ambiente de testes (issue DADOS-01). Todas as instruções são
-- condicionais: em um banco novo não alteram nada.

-- 1. Nome do produto principal com erro de digitação.
UPDATE product SET name = 'Frango Assado', updated_at = NOW()
WHERE name = 'Frango Asssado';

-- 2. Lotes com saldo em produtos que não têm estoque: o ajuste antigo zerou o produto sem baixar
--    o lote (ex.: "QA Perecivel 20261111-140826", 2 un no lote e 0 no produto).
UPDATE inventory_lot lot SET quantity = 0
WHERE lot.quantity > 0
  AND EXISTS (SELECT 1 FROM product_stock stock WHERE stock.product_id = lot.product_id AND stock.quantity = 0);

-- 3. Vendas e caixas antigos gravados com o operador do seed de desenvolvimento, que não existe
--    na tabela de usuários: passam a apontar para a Administradora cadastrada.
UPDATE sale
SET operator_id = admin.id,
    operator_name = COALESCE(sale.operator_name, admin.name)
FROM (SELECT id, name FROM tb_users WHERE email = 'admin@dafabi.com.br') admin
WHERE sale.operator_id = '00000000-0000-0000-0000-000000000101'
  AND NOT EXISTS (SELECT 1 FROM tb_users WHERE id = '00000000-0000-0000-0000-000000000101');

UPDATE cash_register
SET operator_id = admin.id
FROM (SELECT id FROM tb_users WHERE email = 'admin@dafabi.com.br') admin
WHERE cash_register.operator_id = '00000000-0000-0000-0000-000000000101'
  AND NOT EXISTS (SELECT 1 FROM tb_users WHERE id = '00000000-0000-0000-0000-000000000101');

-- 4. Produtos e fornecedores criados em testes de QA saem das listas de uso.
UPDATE product SET status = 'INACTIVE', updated_at = NOW()
WHERE status = 'ACTIVE' AND (name LIKE 'QA %' OR name LIKE 'QA UX %');

UPDATE supplier SET active = false, updated_at = NOW()
WHERE active = true AND name LIKE 'QA %';
