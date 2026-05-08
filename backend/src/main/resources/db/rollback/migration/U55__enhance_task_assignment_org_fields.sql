-- Input: migration/V55__enhance_task_assignment_org_fields.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_tasks_assignee_role_code;
DROP INDEX IF EXISTS idx_tasks_assignee_dept_code;
ALTER TABLE tasks DROP COLUMN IF EXISTS assignee_role_name;
ALTER TABLE tasks DROP COLUMN IF EXISTS assignee_role_code;
ALTER TABLE tasks DROP COLUMN IF EXISTS assignee_dept_name;
ALTER TABLE tasks DROP COLUMN IF EXISTS assignee_dept_code;
