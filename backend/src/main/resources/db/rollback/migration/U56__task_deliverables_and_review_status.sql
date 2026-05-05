-- Input: migration/V56__task_deliverables_and_review_status.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_task_deliverables_type;
DROP INDEX IF EXISTS idx_task_deliverables_task_id;
DROP TABLE IF EXISTS task_deliverables;
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_status_check;
-- Manual rollback required: source migration dropped tasks.constraint.
