-- V7: Criação da tabela de despesas.

CREATE TABLE IF NOT EXISTS expense (
    id UUID PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    expense_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    observation VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_expense_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_expense_payment_method CHECK (
        payment_method IN ('CASH', 'PIX', 'DEBIT', 'CREDIT', 'BOLETO_TRANSFER')
    ),
    CONSTRAINT ck_expense_status CHECK (
        status IN ('PENDING', 'CONSOLIDATED', 'CANCELLED')
    )
);

CREATE INDEX IF NOT EXISTS ix_expense_date ON expense (expense_date);
CREATE INDEX IF NOT EXISTS ix_expense_status ON expense (status);
CREATE INDEX IF NOT EXISTS ix_expense_user ON expense (user_id);