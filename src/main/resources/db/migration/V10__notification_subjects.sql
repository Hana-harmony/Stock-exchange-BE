ALTER TABLE notification_items
    ALTER COLUMN event_id DROP NOT NULL;

ALTER TABLE notification_items
    ALTER COLUMN primary_stock_code DROP NOT NULL;

ALTER TABLE notification_items
    ADD COLUMN subject_type VARCHAR(40);

ALTER TABLE notification_items
    ADD COLUMN subject_id VARCHAR(64);

UPDATE notification_items
SET subject_type = 'ALERT_EVENT',
    subject_id = event_id
WHERE subject_type IS NULL;

ALTER TABLE notification_items
    ALTER COLUMN subject_type SET NOT NULL;

ALTER TABLE notification_items
    ALTER COLUMN subject_id SET NOT NULL;

CREATE UNIQUE INDEX uq_notification_items_subject_account
    ON notification_items(subject_type, subject_id, account_id);
