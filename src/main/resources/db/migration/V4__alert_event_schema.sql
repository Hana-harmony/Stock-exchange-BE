CREATE TABLE alert_events (
    event_id VARCHAR(64) PRIMARY KEY,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    source_type VARCHAR(40) NOT NULL,
    title VARCHAR(300) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    original_url VARCHAR(1000) NOT NULL,
    stock_code CHAR(6) NOT NULL,
    sentiment VARCHAR(40) NOT NULL,
    importance VARCHAR(40) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    watchlist_target BOOLEAN NOT NULL,
    holder_target BOOLEAN NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    matched_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_alert_events_stock_published
    ON alert_events(stock_code, published_at DESC);

CREATE TABLE alert_event_related_stocks (
    event_id VARCHAR(64) NOT NULL REFERENCES alert_events(event_id) ON DELETE CASCADE,
    stock_code CHAR(6) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (event_id, stock_code)
);

CREATE INDEX idx_alert_event_related_stocks_stock
    ON alert_event_related_stocks(stock_code, event_id);

CREATE TABLE alert_event_targets (
    event_id VARCHAR(64) NOT NULL REFERENCES alert_events(event_id) ON DELETE CASCADE,
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (event_id, account_id)
);

CREATE TABLE alert_event_target_reasons (
    event_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(16) NOT NULL,
    match_reason VARCHAR(40) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (event_id, account_id, match_reason),
    FOREIGN KEY (event_id, account_id) REFERENCES alert_event_targets(event_id, account_id) ON DELETE CASCADE
);

CREATE TABLE alert_event_target_stocks (
    event_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(16) NOT NULL,
    stock_code CHAR(6) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (event_id, account_id, stock_code),
    FOREIGN KEY (event_id, account_id) REFERENCES alert_event_targets(event_id, account_id) ON DELETE CASCADE
);
