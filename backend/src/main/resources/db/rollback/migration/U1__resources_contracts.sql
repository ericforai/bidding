-- Input: migration/V1__resources_contracts.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_bar_certificate_borrow_records_status;
DROP INDEX IF EXISTS idx_bar_certificate_borrow_records_certificate_id;
DROP TABLE IF EXISTS bar_certificate_borrow_records;
DROP INDEX IF EXISTS idx_bar_certificates_status;
DROP INDEX IF EXISTS idx_bar_certificates_asset_id;
DROP TABLE IF EXISTS bar_certificates;
DROP INDEX IF EXISTS idx_expense_payment_records_paid_at;
DROP INDEX IF EXISTS idx_expense_payment_records_expense_id;
DROP TABLE IF EXISTS expense_payment_records;
DROP INDEX IF EXISTS idx_expense_approval_records_acted_at;
DROP INDEX IF EXISTS idx_expense_approval_records_expense_id;
DROP TABLE IF EXISTS expense_approval_records;
-- Manual rollback required for column alteration on expenses.status.
-- Data rollback required for UPDATE expenses; original values are not stored in migration history.
-- Data rollback required for UPDATE expenses; original values are not stored in migration history.
ALTER TABLE expenses DROP COLUMN IF EXISTS return_comment;
ALTER TABLE expenses DROP COLUMN IF EXISTS return_confirmed_at;
ALTER TABLE expenses DROP COLUMN IF EXISTS return_requested_at;
ALTER TABLE expenses DROP COLUMN IF EXISTS approved_at;
ALTER TABLE expenses DROP COLUMN IF EXISTS approved_by;
ALTER TABLE expenses DROP COLUMN IF EXISTS approval_comment;
ALTER TABLE expenses DROP COLUMN IF EXISTS status;
ALTER TABLE expenses DROP COLUMN IF EXISTS expense_type;
