-- Input: migration/V50__create_bid_result_tables.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_bid_result_sync_type;
DROP TABLE IF EXISTS bid_result_sync_logs;
DROP INDEX IF EXISTS idx_bid_result_reminder_status;
DROP INDEX IF EXISTS idx_bid_result_reminder_project;
DROP TABLE IF EXISTS bid_result_reminders;
DROP INDEX IF EXISTS idx_bid_result_fetch_tender;
DROP INDEX IF EXISTS idx_bid_result_fetch_project;
DROP INDEX IF EXISTS idx_bid_result_fetch_status;
DROP TABLE IF EXISTS bid_result_fetch_results;
