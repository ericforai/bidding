-- Input: migration/V7__case_advanced_capabilities.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_case_reference_records_case_id_referenced_at;
DROP TABLE IF EXISTS case_reference_records;
DROP INDEX IF EXISTS idx_case_share_records_case_id_created_at;
DROP TABLE IF EXISTS case_share_records;
DROP TABLE IF EXISTS case_technologies;
DROP TABLE IF EXISTS case_highlights;
DROP TABLE IF EXISTS case_tags;
ALTER TABLE cases DROP COLUMN IF EXISTS use_count;
ALTER TABLE cases DROP COLUMN IF EXISTS view_count;
ALTER TABLE cases DROP COLUMN IF EXISTS project_period;
ALTER TABLE cases DROP COLUMN IF EXISTS location_name;
ALTER TABLE cases DROP COLUMN IF EXISTS customer_name;
