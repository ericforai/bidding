-- Input: migration/V3__project_detail_workflow_contracts.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_project_share_links_project;
DROP INDEX IF EXISTS idx_project_reminders_project;
DROP INDEX IF EXISTS idx_project_documents_project;
DROP TABLE IF EXISTS project_share_links;
DROP TABLE IF EXISTS project_reminders;
DROP TABLE IF EXISTS project_documents;
