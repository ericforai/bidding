-- Input: migration/V16__create_project_group_tables.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE project_group_projects DROP CONSTRAINT IF EXISTS fk_project_group_projects_project;
ALTER TABLE project_group_projects DROP CONSTRAINT IF EXISTS fk_project_group_projects_group;
ALTER TABLE project_group_role_access DROP CONSTRAINT IF EXISTS fk_project_group_role_access_group;
ALTER TABLE project_group_members DROP CONSTRAINT IF EXISTS fk_project_group_members_user;
ALTER TABLE project_group_members DROP CONSTRAINT IF EXISTS fk_project_group_members_group;
ALTER TABLE project_groups DROP CONSTRAINT IF EXISTS fk_project_groups_manager_user;
DROP INDEX IF EXISTS idx_project_group_projects_project_id;
DROP TABLE IF EXISTS project_group_projects;
DROP TABLE IF EXISTS project_group_role_access;
DROP TABLE IF EXISTS project_group_members;
DROP INDEX IF EXISTS idx_project_groups_manager_user_id;
DROP TABLE IF EXISTS project_groups;
