CREATE TABLE notification_items (
    notification_id VARCHAR(16) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    event_id VARCHAR(64) NOT NULL REFERENCES alert_events(event_id) ON DELETE CASCADE,
    source_type VARCHAR(40) NOT NULL,
    title VARCHAR(300) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    original_url VARCHAR(1000) NOT NULL,
    primary_stock_code CHAR(6) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    delivery_provider VARCHAR(40),
    delivery_attempt_count INTEGER NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE,
    last_delivery_error VARCHAR(1000),
    read BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (event_id, account_id)
);

CREATE INDEX idx_notification_items_account_created
    ON notification_items(account_id, created_at DESC);

CREATE TABLE notification_matched_stocks (
    notification_id VARCHAR(16) NOT NULL REFERENCES notification_items(notification_id) ON DELETE CASCADE,
    stock_code CHAR(6) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (notification_id, stock_code)
);

CREATE TABLE notification_match_reasons (
    notification_id VARCHAR(16) NOT NULL REFERENCES notification_items(notification_id) ON DELETE CASCADE,
    match_reason VARCHAR(40) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (notification_id, match_reason)
);
