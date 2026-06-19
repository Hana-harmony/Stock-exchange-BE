ALTER TABLE notification_device_tokens
    ADD COLUMN encrypted_token VARCHAR(4096);
