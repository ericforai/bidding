-- Input: migration/V94__wecom_push_binding_and_log.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_outbound_log_user_created;
DROP INDEX IF EXISTS idx_outbound_log_notification;
DROP TABLE IF EXISTS notification_outbound_log;
DROP INDEX IF EXISTS uk_users_wecom_user_id;
ALTER TABLE users DROP COLUMN IF EXISTS wecom_user_id;
