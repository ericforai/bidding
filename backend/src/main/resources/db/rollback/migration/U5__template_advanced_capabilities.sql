-- Input: migration/V5__template_advanced_capabilities.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_template_download_records_template_id_downloaded_at;
DROP TABLE IF EXISTS template_download_records;
DROP INDEX IF EXISTS idx_template_use_records_template_id_used_at;
DROP TABLE IF EXISTS template_use_records;
DROP INDEX IF EXISTS idx_template_versions_template_id_created_at;
DROP TABLE IF EXISTS template_versions;
-- Data rollback required for UPDATE templates; original values are not stored in migration history.
ALTER TABLE templates DROP COLUMN IF EXISTS file_size;
ALTER TABLE templates DROP COLUMN IF EXISTS current_version;
ALTER TABLE templates DROP COLUMN IF EXISTS description;
