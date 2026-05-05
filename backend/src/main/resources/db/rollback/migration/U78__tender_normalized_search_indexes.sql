-- Input: migration/V78__tender_normalized_search_indexes.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_tender_status_region_industry_normalized;
DROP INDEX IF EXISTS idx_tender_purchaser_hash_normalized;
DROP INDEX IF EXISTS idx_tender_industry_normalized;
DROP INDEX IF EXISTS idx_tender_region_normalized;
DROP INDEX IF EXISTS idx_tender_source_normalized;
-- Data rollback required for UPDATE tenders; original values are not stored in migration history.
ALTER TABLE tenders DROP COLUMN IF EXISTS search_text_normalized;
ALTER TABLE tenders DROP COLUMN IF EXISTS purchaser_name_normalized;
ALTER TABLE tenders DROP COLUMN IF EXISTS purchaser_hash_normalized;
ALTER TABLE tenders DROP COLUMN IF EXISTS industry_normalized;
ALTER TABLE tenders DROP COLUMN IF EXISTS region_normalized;
ALTER TABLE tenders DROP COLUMN IF EXISTS source_normalized;
