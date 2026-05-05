-- Input: migration/V92__notification_inbox.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_user_notification_user_created;
DROP INDEX IF EXISTS idx_user_notification_user_read;
DROP TABLE IF EXISTS user_notification;
DROP INDEX IF EXISTS idx_notification_created;
DROP INDEX IF EXISTS idx_notification_source;
DROP TABLE IF EXISTS notification;
