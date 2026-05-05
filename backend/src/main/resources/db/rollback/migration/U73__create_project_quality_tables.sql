-- Input: migration/V73__create_project_quality_tables.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_project_quality_issues_check_id;
DROP TABLE IF EXISTS project_quality_issues;
DROP INDEX IF EXISTS idx_project_quality_checks_project_checked_at;
DROP TABLE IF EXISTS project_quality_checks;
