CREATE TABLE audit_events (
    audit_event_id VARCHAR(16) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    event_type VARCHAR(40) NOT NULL,
    subject_type VARCHAR(40) NOT NULL,
    subject_id VARCHAR(64) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_events_account_occurred
    ON audit_events(account_id, occurred_at DESC);
