-- Input: migration/V72__non_integration_gap_closure_schema.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_tender_assignment_records_assigned_at;
DROP INDEX IF EXISTS idx_tender_assignment_records_assignee;
DROP INDEX IF EXISTS idx_tender_assignment_records_tender;
DROP TABLE IF EXISTS tender_assignment_records;
ALTER TABLE alert_history DROP COLUMN IF EXISTS acknowledged_at;
