CREATE TABLE watchlist_items (
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    stock_code CHAR(6) NOT NULL,
    stock_name VARCHAR(120) NOT NULL,
    market VARCHAR(20) NOT NULL,
    targeting_mode VARCHAR(40) NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (account_id, stock_code)
);

CREATE INDEX idx_watchlist_items_stock_code
    ON watchlist_items(stock_code, account_id);
