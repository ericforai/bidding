-- Input: migration/V46__session_enhancement.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE refresh_sessions DROP COLUMN IF EXISTS last_seen_at;
ALTER TABLE refresh_sessions DROP COLUMN IF EXISTS user_agent;
ALTER TABLE refresh_sessions DROP COLUMN IF EXISTS ip_address;
ALTER TABLE refresh_sessions DROP COLUMN IF EXISTS device_info;
