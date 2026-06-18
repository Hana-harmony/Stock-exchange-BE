CREATE TABLE tax_refund_cases (
    case_id VARCHAR(16) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    tax_year INTEGER NOT NULL,
    treaty_country CHAR(2) NOT NULL,
    residence_certificate_file_name VARCHAR(255) NOT NULL,
    reduced_tax_application_file_name VARCHAR(255) NOT NULL,
    advance_payment_requested BOOLEAN NOT NULL,
    status VARCHAR(40) NOT NULL,
    total_sell_amount_usd NUMERIC(18, 2) NOT NULL,
    realized_profit_usd NUMERIC(18, 2) NOT NULL,
    realized_loss_usd NUMERIC(18, 2) NOT NULL,
    net_realized_pnl_usd NUMERIC(18, 2) NOT NULL,
    taxable_realized_pnl_usd NUMERIC(18, 2) NOT NULL,
    estimated_withholding_tax_usd NUMERIC(18, 2) NOT NULL,
    estimated_treaty_tax_usd NUMERIC(18, 2) NOT NULL,
    estimated_refund_usd NUMERIC(18, 2) NOT NULL,
    advance_payment_eligible BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (account_id, tax_year)
);

CREATE INDEX idx_tax_refund_cases_account_latest
    ON tax_refund_cases(account_id, tax_year DESC, updated_at DESC);

CREATE TABLE tax_refund_case_matched_trades (
    case_id VARCHAR(16) NOT NULL REFERENCES tax_refund_cases(case_id) ON DELETE CASCADE,
    trade_id VARCHAR(16) NOT NULL REFERENCES mock_trade_ledger_entries(trade_id),
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (case_id, trade_id)
);
