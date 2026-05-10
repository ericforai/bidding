-- Input: migration/V108__tender_filter_fields.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_tender_registration_deadline;
DROP INDEX IF EXISTS idx_tender_bid_opening_time;
ALTER TABLE tenders DROP COLUMN IF EXISTS registration_deadline;
