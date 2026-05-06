-- Input: migration/V74__contract_borrow_schema.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_contract_borrow_events_application;
DROP INDEX IF EXISTS idx_contract_borrow_borrower;
DROP INDEX IF EXISTS idx_contract_borrow_expected_return;
DROP INDEX IF EXISTS idx_contract_borrow_status;
DROP TABLE IF EXISTS contract_borrow_events;
DROP TABLE IF EXISTS contract_borrow_applications;
