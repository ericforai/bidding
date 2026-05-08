-- Input: migration/V70__historical_project_snapshots.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_history_snapshot_export;
DROP INDEX IF EXISTS idx_history_snapshot_archive;
DROP INDEX IF EXISTS idx_history_snapshot_project;
DROP TABLE IF EXISTS historical_project_snapshots;
