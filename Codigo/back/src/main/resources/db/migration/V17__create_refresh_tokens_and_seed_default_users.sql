-- V17: Criação idempotente de refresh tokens e perfis padrão para bancos existentes.

CREATE TABLE IF NOT EXISTS tb_refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES tb_users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON tb_refresh_tokens(user_id);

INSERT INTO tb_roles (id, name) VALUES
    ('00000000-0000-0000-0000-000000000201', 'ROLE_ADMIN'),
    ('00000000-0000-0000-0000-000000000202', 'ROLE_CASHIER')
ON CONFLICT (name) DO NOTHING;
