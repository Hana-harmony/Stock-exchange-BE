ALTER TABLE alert_events ADD COLUMN summary_what VARCHAR(500);
ALTER TABLE alert_events ADD COLUMN summary_why VARCHAR(500);
ALTER TABLE alert_events ADD COLUMN summary_impact VARCHAR(500);
ALTER TABLE alert_events ADD COLUMN translated_summary VARCHAR(2000);
ALTER TABLE alert_events ADD COLUMN original_content TEXT;
ALTER TABLE alert_events ADD COLUMN translated_content TEXT;
ALTER TABLE alert_events ADD COLUMN image_urls TEXT;
ALTER TABLE alert_events ADD COLUMN content_availability VARCHAR(40) DEFAULT 'SUMMARY_ONLY' NOT NULL;
ALTER TABLE alert_events ADD COLUMN cluster_key VARCHAR(128);
