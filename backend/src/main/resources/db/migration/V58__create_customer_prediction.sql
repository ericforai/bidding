CREATE TABLE customer_predictions (
    id BIGSERIAL PRIMARY KEY,
    purchaser_hash VARCHAR(64) NOT NULL,
    purchaser_name VARCHAR(255) NOT NULL,
    industry VARCHAR(50),
    region VARCHAR(100),
    opportunity_score INTEGER,
    predicted_category VARCHAR(50),
    predicted_budget_min DECIMAL(14,2),
    predicted_budget_max DECIMAL(14,2),
    predicted_window VARCHAR(20),
    confidence DECIMAL(3,2),
    reasoning_summary TEXT,
    evidence_record_ids VARCHAR(500),
    main_categories VARCHAR(500),
    avg_budget DECIMAL(14,2),
    cycle_type VARCHAR(50),
    frequency INTEGER,
    period_months VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'WATCH',
    converted_project_id BIGINT,
    sales_rep VARCHAR(100),
    last_computed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_cp_purchaser_hash ON customer_predictions(purchaser_hash);
CREATE INDEX idx_cp_status ON customer_predictions(status);
CREATE INDEX idx_cp_opportunity_score ON customer_predictions(opportunity_score);
