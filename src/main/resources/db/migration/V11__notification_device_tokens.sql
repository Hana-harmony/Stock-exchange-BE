CREATE TABLE notification_device_tokens (
    device_token_id VARCHAR(16) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    platform VARCHAR(20) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    masked_token VARCHAR(80) NOT NULL,
    app_version VARCHAR(40),
    locale VARCHAR(10),
    active BOOLEAN NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    disabled_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (account_id, platform, token_hash)
);

CREATE INDEX idx_notification_device_tokens_account_active
    ON notification_device_tokens(account_id, active, last_seen_at DESC);
