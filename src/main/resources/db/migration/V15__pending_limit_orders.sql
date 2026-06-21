CREATE TABLE pending_limit_orders (
    order_id VARCHAR(16) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    stock_code CHAR(6) NOT NULL,
    stock_name VARCHAR(120) NOT NULL,
    side VARCHAR(4) NOT NULL,
    quantity BIGINT NOT NULL,
    limit_price_usd NUMERIC(18, 2) NOT NULL,
    observed_price_usd NUMERIC(18, 2) NOT NULL,
    status VARCHAR(12) NOT NULL,
    trade_id VARCHAR(16) REFERENCES mock_trade_ledger_entries(trade_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    filled_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_pending_limit_orders_account_created_at
    ON pending_limit_orders (account_id, created_at DESC);

CREATE INDEX idx_pending_limit_orders_stock_status
    ON pending_limit_orders (stock_code, status);
