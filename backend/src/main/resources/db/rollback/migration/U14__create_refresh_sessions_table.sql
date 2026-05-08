-- Input: migration/V14__create_refresh_sessions_table.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE refresh_sessions DROP CONSTRAINT IF EXISTS fk_refresh_sessions_user;
DROP INDEX IF EXISTS idx_refresh_sessions_expires_at;
DROP INDEX IF EXISTS idx_refresh_sessions_user_id;
DROP TABLE IF EXISTS refresh_sessions;
