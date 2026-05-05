-- Input: migration/V69__deposit_return_followup.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE alert_rules DROP CONSTRAINT IF EXISTS alert_rules_type_check;
-- Manual rollback required: source migration dropped alert_rules.CONSTRAINT.
ALTER TABLE expenses DROP COLUMN IF EXISTS last_return_reminder_at;
ALTER TABLE expenses DROP COLUMN IF EXISTS expected_return_date;
