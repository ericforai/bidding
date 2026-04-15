-- Add customer_manager and customer_manager_id to projects table to support dual project manager model
ALTER TABLE projects ADD COLUMN customer_manager VARCHAR(100);
ALTER TABLE projects ADD COLUMN customer_manager_id VARCHAR(100);
