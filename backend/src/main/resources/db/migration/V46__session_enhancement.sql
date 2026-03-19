-- Add session tracking fields to refresh_sessions table
ALTER TABLE refresh_sessions ADD COLUMN IF NOT EXISTS device_info VARCHAR(255);
ALTER TABLE refresh_sessions ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);
ALTER TABLE refresh_sessions ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500);
ALTER TABLE refresh_sessions ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMP;
