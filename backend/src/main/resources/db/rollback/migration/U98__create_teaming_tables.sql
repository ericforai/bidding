-- Input: migration/V98__create_teaming_tables.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_project_member_user;
DROP INDEX IF EXISTS idx_project_member_project;
DROP TABLE IF EXISTS sys_project_member;
DROP INDEX IF EXISTS idx_crm_perm_user;
DROP INDEX IF EXISTS idx_crm_perm_customer;
DROP TABLE IF EXISTS sys_crm_customer_permission;
