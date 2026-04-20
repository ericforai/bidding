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
