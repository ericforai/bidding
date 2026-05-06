-- Input: migration/V53__add_project_analysis_columns.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE projects DROP COLUMN IF EXISTS ai_analysis_json;
ALTER TABLE projects DROP COLUMN IF EXISTS tasks_json;
ALTER TABLE projects DROP COLUMN IF EXISTS competitor_analysis_json;
