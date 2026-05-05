-- Input: migration/V97__workflow_form_designer.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE workflow_form_instances DROP COLUMN IF EXISTS oa_payload_json;
ALTER TABLE workflow_form_instances DROP COLUMN IF EXISTS oa_binding_snapshot_json;
ALTER TABLE workflow_form_instances DROP COLUMN IF EXISTS schema_snapshot_json;
ALTER TABLE workflow_form_instances DROP COLUMN IF EXISTS template_version;
-- Data rollback required for INSERT INTO workflow_form_template_versions; verify seed rows before deleting.
-- Data rollback required for INSERT INTO workflow_form_template_drafts; verify seed rows before deleting.
DROP INDEX IF EXISTS idx_workflow_form_template_versions_code;
DROP TABLE IF EXISTS workflow_form_template_versions;
DROP TABLE IF EXISTS workflow_form_template_drafts;
