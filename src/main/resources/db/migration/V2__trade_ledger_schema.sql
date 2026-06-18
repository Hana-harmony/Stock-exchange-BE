CREATE TABLE mock_holdings (
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    stock_code CHAR(6) NOT NULL,
    stock_name VARCHAR(120) NOT NULL,
    quantity BIGINT NOT NULL,
    average_price_usd NUMERIC(18, 2) NOT NULL,
    cost_basis_usd NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (account_id, stock_code)
);

CREATE INDEX idx_mock_holdings_stock_code
    ON mock_holdings(stock_code, account_id);

CREATE TABLE mock_trade_ledger_entries (
    trade_id VARCHAR(16) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    stock_code CHAR(6) NOT NULL,
    stock_name VARCHAR(120) NOT NULL,
    side VARCHAR(4) NOT NULL,
    quantity BIGINT NOT NULL,
    execution_price_usd NUMERIC(18, 2) NOT NULL,
    gross_amount_usd NUMERIC(18, 2) NOT NULL,
    realized_pnl_usd NUMERIC(18, 2) NOT NULL,
    remaining_quantity BIGINT NOT NULL,
    average_price_usd_after NUMERIC(18, 2) NOT NULL,
    cash_balance_usd_after NUMERIC(18, 2) NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_mock_trade_ledger_entries_account_executed
    ON mock_trade_ledger_entries(account_id, executed_at DESC);

CREATE INDEX idx_mock_trade_ledger_entries_stock_code
    ON mock_trade_ledger_entries(stock_code, account_id);
