-- Input: migration/V64__bid_result_closure_enhancement.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE bid_result_fetch_results DROP COLUMN IF EXISTS win_announce_doc_url;
ALTER TABLE bid_result_fetch_results DROP COLUMN IF EXISTS sku_count;
ALTER TABLE bid_result_fetch_results DROP COLUMN IF EXISTS remark;
ALTER TABLE bid_result_fetch_results DROP COLUMN IF EXISTS contract_duration_months;
ALTER TABLE bid_result_fetch_results DROP COLUMN IF EXISTS contract_end_date;
ALTER TABLE bid_result_fetch_results DROP COLUMN IF EXISTS contract_start_date;
ALTER TABLE bid_result_fetch_results DROP COLUMN IF EXISTS registration_type;
