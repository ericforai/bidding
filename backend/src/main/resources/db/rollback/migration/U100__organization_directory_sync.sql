-- Input: migration/V100__organization_directory_sync.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_org_sync_items_external_dept;
DROP INDEX IF EXISTS idx_org_sync_items_external_user;
DROP INDEX IF EXISTS idx_org_sync_items_run_status;
DROP INDEX IF EXISTS idx_org_sync_items_run;
DROP TABLE IF EXISTS organization_sync_items;
DROP INDEX IF EXISTS idx_org_sync_runs_started_at;
DROP INDEX IF EXISTS idx_org_sync_runs_source_status;
DROP TABLE IF EXISTS organization_sync_runs;
DROP INDEX IF EXISTS idx_users_external_org_user;
ALTER TABLE users DROP COLUMN IF EXISTS last_org_synced_at;
ALTER TABLE users DROP COLUMN IF EXISTS last_org_event_key;
ALTER TABLE users DROP COLUMN IF EXISTS external_org_source_app;
ALTER TABLE users DROP COLUMN IF EXISTS external_org_user_id;
DROP INDEX IF EXISTS idx_org_departments_parent_external;
DROP INDEX IF EXISTS idx_org_departments_external;
ALTER TABLE organization_departments DROP COLUMN IF EXISTS last_synced_at;
ALTER TABLE organization_departments DROP COLUMN IF EXISTS last_event_key;
ALTER TABLE organization_departments DROP COLUMN IF EXISTS source_app;
ALTER TABLE organization_departments DROP COLUMN IF EXISTS parent_external_dept_id;
ALTER TABLE organization_departments DROP COLUMN IF EXISTS external_dept_id;
DROP INDEX IF EXISTS idx_org_event_logs_next_retry;
DROP INDEX IF EXISTS idx_org_event_logs_external_dept;
DROP INDEX IF EXISTS idx_org_event_logs_external_user;
DROP INDEX IF EXISTS idx_org_event_logs_upstream_key;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS last_error_code;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS next_retry_at;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS retry_count;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS raw_payload;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS external_dept_id;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS external_user_id;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS entity_type;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS event_time;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS parent_id;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS span_id;
ALTER TABLE organization_event_logs DROP COLUMN IF EXISTS upstream_event_key;
