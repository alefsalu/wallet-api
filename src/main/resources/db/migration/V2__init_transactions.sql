CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_after NUMERIC(12, 2) NOT NULL,
    counterparty_wallet_id UUID,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_transactions_wallet_id_created_at ON transactions(wallet_id, created_at DESC);
