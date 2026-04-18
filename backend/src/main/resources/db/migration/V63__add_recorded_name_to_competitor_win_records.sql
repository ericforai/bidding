-- Add recorded_name column to competitor_win_records if it's missing
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='competitor_win_records' AND column_name='recorded_name') THEN
        ALTER TABLE competitor_win_records ADD COLUMN recorded_name VARCHAR(100);
    END IF;
END $$;
