ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS expense_type VARCHAR(100);

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS status VARCHAR(50);

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS approval_comment VARCHAR(500);

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100);

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS return_requested_at TIMESTAMP;

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS return_confirmed_at TIMESTAMP;

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS return_comment VARCHAR(500);

UPDATE expenses
SET expense_type = '其他'
WHERE expense_type IS NULL;

UPDATE expenses
SET status = 'PAID'
WHERE status IS NULL;

ALTER TABLE expenses
    ALTER COLUMN status SET NOT NULL;

CREATE TABLE IF NOT EXISTS expense_approval_records (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    result VARCHAR(30) NOT NULL,
    comment VARCHAR(500),
    approver VARCHAR(100) NOT NULL,
    acted_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_expense_approval_records_expense
        FOREIGN KEY (expense_id) REFERENCES expenses(id)
);

CREATE INDEX IF NOT EXISTS idx_expense_approval_records_expense_id
    ON expense_approval_records(expense_id);

CREATE INDEX IF NOT EXISTS idx_expense_approval_records_acted_at
    ON expense_approval_records(acted_at);

CREATE TABLE IF NOT EXISTS expense_payment_records (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    paid_by VARCHAR(100) NOT NULL,
    payment_reference VARCHAR(100),
    payment_method VARCHAR(50),
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expense_payment_records_expense
        FOREIGN KEY (expense_id) REFERENCES expenses(id)
);

CREATE INDEX IF NOT EXISTS idx_expense_payment_records_expense_id
    ON expense_payment_records(expense_id);

CREATE INDEX IF NOT EXISTS idx_expense_payment_records_paid_at
    ON expense_payment_records(paid_at);

CREATE TABLE IF NOT EXISTS bar_certificates (
    id BIGSERIAL PRIMARY KEY,
    bar_asset_id BIGINT NOT NULL,
    type VARCHAR(100) NOT NULL,
    provider VARCHAR(100),
    serial_no VARCHAR(200) NOT NULL,
    holder VARCHAR(100),
    location VARCHAR(200),
    expiry_date DATE,
    status VARCHAR(30) NOT NULL,
    current_borrower VARCHAR(100),
    current_project_id BIGINT,
    borrow_purpose VARCHAR(200),
    expected_return_date DATE,
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_bar_certificates_asset
        FOREIGN KEY (bar_asset_id) REFERENCES bar_assets(id)
);

CREATE INDEX IF NOT EXISTS idx_bar_certificates_asset_id
    ON bar_certificates(bar_asset_id);

CREATE INDEX IF NOT EXISTS idx_bar_certificates_status
    ON bar_certificates(status);

CREATE TABLE IF NOT EXISTS bar_certificate_borrow_records (
    id BIGSERIAL PRIMARY KEY,
    certificate_id BIGINT NOT NULL,
    borrower VARCHAR(100) NOT NULL,
    project_id BIGINT,
    purpose VARCHAR(200),
    remark VARCHAR(500),
    borrowed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expected_return_date DATE,
    returned_at TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_bar_certificate_borrow_records_certificate
        FOREIGN KEY (certificate_id) REFERENCES bar_certificates(id)
);

CREATE INDEX IF NOT EXISTS idx_bar_certificate_borrow_records_certificate_id
    ON bar_certificate_borrow_records(certificate_id);

CREATE INDEX IF NOT EXISTS idx_bar_certificate_borrow_records_status
    ON bar_certificate_borrow_records(status);
