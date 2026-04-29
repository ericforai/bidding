-- Seed default admin user for first-run / private deployment scenarios.
-- Password: XiyuAdmin2026! (must be changed after deployment)
-- Idempotent via INSERT...SELECT WHERE NOT EXISTS.

INSERT INTO users (username, password, email, full_name, role, enabled, email_verified, role_id, created_at, updated_at)
SELECT 'admin',
       '$2a$10$iBPGarpbIRXFiKDhGGlWHuXLyai7k9XHLKzlYgJcufBqoA9bH88i6',
       'admin@xiyu-local',
       '系统管理员',
       'ADMIN',
       TRUE,
       TRUE,
       (SELECT id FROM roles WHERE code = 'admin'),
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
