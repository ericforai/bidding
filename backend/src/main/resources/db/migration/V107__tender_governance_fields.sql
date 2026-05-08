ALTER TABLE tenders ADD COLUMN IF NOT EXISTS tender_agency VARCHAR(255);
ALTER TABLE tenders ADD COLUMN IF NOT EXISTS bid_opening_time TIMESTAMP;
ALTER TABLE tenders ADD COLUMN IF NOT EXISTS customer_type VARCHAR(100);
ALTER TABLE tenders ADD COLUMN IF NOT EXISTS priority VARCHAR(10);

CREATE INDEX IF NOT EXISTS idx_tender_customer_type ON tenders (customer_type);
CREATE INDEX IF NOT EXISTS idx_tender_priority ON tenders (priority);
