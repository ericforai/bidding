-- V95 inbox P1: subscription + mention tables
--
-- 新增：
--   1. subscription  — 多态关注（user 关注 任意实体）
--   2. mention       — @ 提及记录（审计 + 源实体关联）
--
-- 依赖 V92 的 notification 表。

CREATE TABLE subscription (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_entity_type VARCHAR(50) NOT NULL,
    target_entity_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_subscription_user_target UNIQUE (user_id, target_entity_type, target_entity_id)
);

CREATE INDEX idx_subscription_target
    ON subscription(target_entity_type, target_entity_id);

CREATE INDEX idx_subscription_user
    ON subscription(user_id);

CREATE TABLE mention (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notification(id) ON DELETE CASCADE,
    mentioner_user_id BIGINT NOT NULL,
    mentioned_user_id BIGINT NOT NULL,
    source_entity_type VARCHAR(50),
    source_entity_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mention_mentioned
    ON mention(mentioned_user_id);

CREATE INDEX idx_mention_source
    ON mention(source_entity_type, source_entity_id);
