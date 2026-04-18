-- Add recorded_name column to competitor_win_records if it's missing
-- Uses ADD COLUMN IF NOT EXISTS (supported by PostgreSQL 9.6+ and H2 2.x) for cross-DB compatibility
ALTER TABLE competitor_win_records ADD COLUMN IF NOT EXISTS recorded_name VARCHAR(100);
