-- Input: migration/V43__password_reset.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE users DROP COLUMN IF EXISTS email_verified;
DROP INDEX IF EXISTS idx_password_reset_expires;
DROP INDEX IF EXISTS idx_password_reset_user;
DROP INDEX IF EXISTS idx_password_reset_token;
DROP TABLE IF EXISTS password_reset_tokens;
