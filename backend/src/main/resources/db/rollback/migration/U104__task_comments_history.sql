-- Input: migration/V104__task_comments_history.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_task_history_archive;
DROP INDEX IF EXISTS idx_task_history_task_created;
DROP TABLE IF EXISTS task_history;
DROP INDEX IF EXISTS idx_task_comment_author;
DROP INDEX IF EXISTS idx_task_comment_task_created;
DROP TABLE IF EXISTS task_comment;
