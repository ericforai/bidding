ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS expected_return_date DATE;

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS last_return_reminder_at TIMESTAMP;

ALTER TABLE alert_rules
    DROP CONSTRAINT IF EXISTS alert_rules_type_check;

ALTER TABLE alert_rules
    ADD CONSTRAINT alert_rules_type_check
        CHECK (type IN ('DEADLINE', 'BUDGET', 'RISK', 'DOCUMENT', 'QUALIFICATION_EXPIRY', 'DEPOSIT_RETURN'));
