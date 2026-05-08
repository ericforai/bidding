-- Input: migration/V44__email_verification.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_email_verify_expires;
DROP INDEX IF EXISTS idx_email_verify_user;
DROP INDEX IF EXISTS idx_email_verify_token;
DROP TABLE IF EXISTS email_verification_tokens;
