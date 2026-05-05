-- Input: migration/V80__tender_upload_queue.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_tender_task_dlq_failed_at;
DROP TABLE IF EXISTS tender_task_dlq;
DROP INDEX IF EXISTS idx_tender_task_locked_status;
DROP INDEX IF EXISTS idx_tender_task_status_available_priority;
DROP TABLE IF EXISTS tender_task;
DROP INDEX IF EXISTS idx_tender_file_user_status_created;
DROP INDEX IF EXISTS uk_tender_file_user_sha256;
DROP TABLE IF EXISTS tender_file;
