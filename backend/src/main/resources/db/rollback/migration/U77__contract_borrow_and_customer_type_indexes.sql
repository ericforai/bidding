-- Input: migration/V77__contract_borrow_and_customer_type_indexes.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_contract_borrow_submitted_at;
DROP INDEX IF EXISTS idx_contract_borrow_status_expected_return;
DROP INDEX IF EXISTS idx_project_customer_type_status;
ALTER TABLE contract_borrow_applications DROP COLUMN IF EXISTS version;
