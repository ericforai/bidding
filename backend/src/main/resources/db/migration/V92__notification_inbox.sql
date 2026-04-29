-- Notification content (one row per notification)
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    source_entity_type VARCHAR(50),
    source_entity_id BIGINT,
    title VARCHAR(200) NOT NULL,
    body TEXT,
    payload_json JSONB,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_source ON notification(source_entity_type, source_entity_id);
CREATE INDEX idx_notification_created ON notification(created_at DESC);

-- Per-user notification state
CREATE TABLE user_notification (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notification(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (notification_id, user_id)
);

CREATE INDEX idx_user_notification_user_read ON user_notification(user_id, read_at);
CREATE INDEX idx_user_notification_user_created ON user_notification(user_id, created_at DESC);
