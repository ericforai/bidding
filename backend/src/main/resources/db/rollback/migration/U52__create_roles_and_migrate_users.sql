-- Input: migration/V52__create_roles_and_migrate_users.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_role_id;
-- Manual rollback required for column alteration on users.role_id.
-- Data rollback required for UPDATE users; original values are not stored in migration history.
-- Data rollback required for UPDATE users; original values are not stored in migration history.
ALTER TABLE users DROP COLUMN IF EXISTS role_id;
-- Data rollback required for INSERT INTO roles; verify seed rows before deleting.
DROP TABLE IF EXISTS roles;
