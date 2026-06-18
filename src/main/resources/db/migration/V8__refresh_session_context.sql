ALTER TABLE refresh_sessions
    ADD COLUMN issued_ip_address VARCHAR(64) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE refresh_sessions
    ADD COLUMN issued_user_agent VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN';
