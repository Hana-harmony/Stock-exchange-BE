CREATE TABLE exchange_users (
    user_id VARCHAR(16) PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    password_salt VARCHAR(256) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE mock_usd_accounts (
    account_id VARCHAR(16) PRIMARY KEY,
    user_id VARCHAR(16) NOT NULL UNIQUE REFERENCES exchange_users(user_id),
    currency CHAR(3) NOT NULL,
    cash_balance_usd NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE mock_cash_ledger_entries (
    ledger_entry_id VARCHAR(16) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    ledger_type VARCHAR(40) NOT NULL,
    amount_usd NUMERIC(18, 2) NOT NULL,
    balance_after_usd NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_mock_cash_ledger_entries_account_created
    ON mock_cash_ledger_entries(account_id, created_at DESC);

CREATE TABLE refresh_sessions (
    session_id VARCHAR(16) PRIMARY KEY,
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    refresh_token_hash VARCHAR(128) NOT NULL UNIQUE,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_session_id VARCHAR(16)
);

CREATE INDEX idx_refresh_sessions_user_active
    ON refresh_sessions(user_id, revoked_at, expires_at);
