-- E-mail do fornecedor passa a ser opcional; o índice único segue valendo para e-mails informados.
ALTER TABLE supplier ALTER COLUMN email DROP NOT NULL;
