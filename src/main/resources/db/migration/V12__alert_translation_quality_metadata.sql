CREATE TABLE alert_event_glossary_terms (
    event_id VARCHAR(64) NOT NULL REFERENCES alert_events(event_id) ON DELETE CASCADE,
    source_term VARCHAR(80) NOT NULL,
    normalized_term VARCHAR(80) NOT NULL,
    english_term VARCHAR(120) NOT NULL,
    category VARCHAR(40) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (event_id, sort_order)
);

CREATE TABLE alert_event_translation_quality_flags (
    event_id VARCHAR(64) NOT NULL REFERENCES alert_events(event_id) ON DELETE CASCADE,
    quality_flag VARCHAR(80) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (event_id, quality_flag)
);

CREATE TABLE notification_glossary_terms (
    notification_id VARCHAR(16) NOT NULL REFERENCES notification_items(notification_id) ON DELETE CASCADE,
    source_term VARCHAR(80) NOT NULL,
    normalized_term VARCHAR(80) NOT NULL,
    english_term VARCHAR(120) NOT NULL,
    category VARCHAR(40) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (notification_id, sort_order)
);

CREATE TABLE notification_translation_quality_flags (
    notification_id VARCHAR(16) NOT NULL REFERENCES notification_items(notification_id) ON DELETE CASCADE,
    quality_flag VARCHAR(80) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (notification_id, quality_flag)
);
