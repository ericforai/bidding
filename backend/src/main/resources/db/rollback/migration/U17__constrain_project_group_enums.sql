-- Input: migration/V17__constrain_project_group_enums.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE project_group_role_access DROP CONSTRAINT IF EXISTS chk_project_group_role_access_role_code;
-- Manual rollback required: source migration dropped project_group_role_access.constraint.
ALTER TABLE project_groups DROP CONSTRAINT IF EXISTS chk_project_groups_visibility;
-- Manual rollback required: source migration dropped project_groups.constraint.
