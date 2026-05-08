-- Input: migration/V106__organization_external_identity_uniques.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS ux_users_external_org_user;
DROP INDEX IF EXISTS ux_org_departments_source_external;
