-- V94 WeCom push notification adaptation
--
-- Adds:
--   1. users.wecom_user_id — maps internal user to 企业微信 userid for personalized push
--   2. notification_outbound_log — observability for outbound notification deliveries

ALTER TABLE users ADD COLUMN wecom_user_id VARCHAR(64);
CREATE UNIQUE INDEX uk_users_wecom_user_id ON users(wecom_user_id);

CREATE TABLE notification_outbound_log (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notification(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    channel VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    skip_reason VARCHAR(50),
    wecom_errcode INTEGER,
    wecom_errmsg VARCHAR(500),
    attempt_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbound_log_notification ON notification_outbound_log(notification_id);
CREATE INDEX idx_outbound_log_user_created ON notification_outbound_log(user_id, created_at DESC);
