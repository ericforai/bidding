-- Input: migration/V57__seed_default_admin_user.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

-- Data rollback required for INSERT INTO users; verify seed rows before deleting.
