-- Input: migration/V12__add_export_tasks_table.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_export_status;
DROP INDEX IF EXISTS idx_export_user;
DROP TABLE IF EXISTS export_tasks;
