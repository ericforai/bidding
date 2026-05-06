-- Input: migration/V93__workflow_form_schema.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

-- Data rollback required for INSERT INTO oa_process_bindings; verify seed rows before deleting.
-- Data rollback required for INSERT INTO workflow_form_templates; verify seed rows before deleting.
DROP INDEX IF EXISTS idx_workflow_form_instances_oa;
DROP INDEX IF EXISTS idx_workflow_form_instances_type_status;
DROP INDEX IF EXISTS idx_workflow_form_instances_project_status;
DROP TABLE IF EXISTS oa_process_events;
DROP TABLE IF EXISTS oa_process_bindings;
DROP TABLE IF EXISTS workflow_form_instances;
DROP TABLE IF EXISTS workflow_form_templates;
