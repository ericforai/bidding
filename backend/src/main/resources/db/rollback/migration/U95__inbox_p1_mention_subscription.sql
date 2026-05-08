-- Input: migration/V95__inbox_p1_mention_subscription.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_mention_source;
DROP INDEX IF EXISTS idx_mention_mentioned;
DROP TABLE IF EXISTS mention;
DROP INDEX IF EXISTS idx_subscription_user;
DROP INDEX IF EXISTS idx_subscription_target;
DROP TABLE IF EXISTS subscription;
