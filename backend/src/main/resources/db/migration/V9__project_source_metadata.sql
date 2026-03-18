ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS source_module VARCHAR(100),
    ADD COLUMN IF NOT EXISTS source_customer_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS source_customer VARCHAR(255),
    ADD COLUMN IF NOT EXISTS source_opportunity_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS source_reasoning_summary TEXT;
