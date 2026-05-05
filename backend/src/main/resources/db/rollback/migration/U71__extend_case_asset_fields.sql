-- Input: migration/V71__extend_case_asset_fields.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_cases_published_at;
DROP INDEX IF EXISTS idx_cases_visibility;
DROP INDEX IF EXISTS idx_cases_status;
DROP INDEX IF EXISTS idx_cases_source_project_id;
DROP INDEX IF EXISTS idx_cases_product_line;
DROP TABLE IF EXISTS case_attachment_names;
DROP TABLE IF EXISTS case_lessons_learned;
DROP TABLE IF EXISTS case_success_factors;
-- Data rollback required for UPDATE cases; original values are not stored in migration history.
ALTER TABLE cases DROP COLUMN IF EXISTS search_document;
ALTER TABLE cases DROP COLUMN IF EXISTS visibility;
ALTER TABLE cases DROP COLUMN IF EXISTS published_at;
ALTER TABLE cases DROP COLUMN IF EXISTS status;
ALTER TABLE cases DROP COLUMN IF EXISTS document_snapshot_text;
ALTER TABLE cases DROP COLUMN IF EXISTS price_strategy;
ALTER TABLE cases DROP COLUMN IF EXISTS archive_summary;
ALTER TABLE cases DROP COLUMN IF EXISTS source_project_id;
ALTER TABLE cases DROP COLUMN IF EXISTS product_line;
