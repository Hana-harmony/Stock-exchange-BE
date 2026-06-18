CREATE TABLE tax_documents (
    document_id VARCHAR(17) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL REFERENCES mock_usd_accounts(account_id),
    user_id VARCHAR(16) NOT NULL REFERENCES exchange_users(user_id),
    document_type VARCHAR(40) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_tax_documents_account_created
    ON tax_documents(account_id, created_at DESC);

ALTER TABLE tax_refund_cases
    ADD COLUMN residence_certificate_document_id VARCHAR(17) REFERENCES tax_documents(document_id);

ALTER TABLE tax_refund_cases
    ADD COLUMN reduced_tax_application_document_id VARCHAR(17) REFERENCES tax_documents(document_id);
