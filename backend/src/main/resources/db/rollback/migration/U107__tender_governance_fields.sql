-- Input: migration/V107__tender_governance_fields.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_tender_priority;
DROP INDEX IF EXISTS idx_tender_customer_type;
ALTER TABLE tenders DROP COLUMN IF EXISTS priority;
ALTER TABLE tenders DROP COLUMN IF EXISTS customer_type;
ALTER TABLE tenders DROP COLUMN IF EXISTS bid_opening_time;
ALTER TABLE tenders DROP COLUMN IF EXISTS tender_agency;
