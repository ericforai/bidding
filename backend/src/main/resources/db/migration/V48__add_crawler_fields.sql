-- Add CEB Crawler fields
ALTER TABLE tenders ADD COLUMN external_id VARCHAR(255);
ALTER TABLE tenders ADD COLUMN original_url VARCHAR(1000);
