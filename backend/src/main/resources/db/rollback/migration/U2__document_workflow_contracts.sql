-- Input: migration/V2__document_workflow_contracts.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_document_reminders_project_section;
DROP INDEX IF EXISTS idx_document_locks_project;
DROP INDEX IF EXISTS idx_document_assignments_project;
DROP TABLE IF EXISTS document_reminders;
DROP TABLE IF EXISTS document_locks;
DROP TABLE IF EXISTS document_assignments;
