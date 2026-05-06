-- Input: migration/V75__tender_search_dimensions.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_tender_status_region_industry;
DROP INDEX IF EXISTS idx_tender_purchaser_hash;
DROP INDEX IF EXISTS idx_tender_industry;
DROP INDEX IF EXISTS idx_tender_region;
ALTER TABLE tenders DROP COLUMN IF EXISTS tags;
ALTER TABLE tenders DROP COLUMN IF EXISTS description;
ALTER TABLE tenders DROP COLUMN IF EXISTS contact_phone;
ALTER TABLE tenders DROP COLUMN IF EXISTS contact_name;
ALTER TABLE tenders DROP COLUMN IF EXISTS publish_date;
ALTER TABLE tenders DROP COLUMN IF EXISTS purchaser_hash;
ALTER TABLE tenders DROP COLUMN IF EXISTS purchaser_name;
ALTER TABLE tenders DROP COLUMN IF EXISTS industry;
ALTER TABLE tenders DROP COLUMN IF EXISTS region;
