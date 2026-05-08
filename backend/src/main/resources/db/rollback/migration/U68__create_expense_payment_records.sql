-- Input: migration/V68__create_expense_payment_records.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_expense_payment_records_paid_at;
DROP INDEX IF EXISTS idx_expense_payment_records_expense_id;
DROP TABLE IF EXISTS expense_payment_records;
