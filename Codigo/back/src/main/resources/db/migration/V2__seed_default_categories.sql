-- Categorias iniciais usadas pelo frontend e pelo cadastro de produtos.
INSERT INTO category (id, name, active) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Bebidas', true),
    ('a0000000-0000-0000-0000-000000000002', 'Complementos', true),
    ('a0000000-0000-0000-0000-000000000003', 'Frangos', true),
    ('a0000000-0000-0000-0000-000000000004', 'Industrializados', true),
    ('a0000000-0000-0000-0000-000000000005', 'Marmitas', true)
ON CONFLICT (id) DO NOTHING;
