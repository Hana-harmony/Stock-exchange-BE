CREATE TABLE portfolio_valuation_snapshots (
    snapshot_id VARCHAR(16) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    currency CHAR(3) NOT NULL,
    cash_balance_usd NUMERIC(18, 2) NOT NULL,
    total_market_value_usd NUMERIC(18, 2) NOT NULL,
    total_asset_value_usd NUMERIC(18, 2) NOT NULL,
    realized_pnl_usd NUMERIC(18, 2) NOT NULL,
    unrealized_pnl_usd NUMERIC(18, 2) NOT NULL,
    holding_count INTEGER NOT NULL,
    valued_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_portfolio_valuation_snapshots_account_valued
    ON portfolio_valuation_snapshots(account_id, valued_at DESC);
