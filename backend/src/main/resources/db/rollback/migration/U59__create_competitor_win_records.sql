-- Input: migration/V59__create_competitor_win_records.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_competitor_win_won_at;
DROP INDEX IF EXISTS idx_competitor_win_project;
DROP INDEX IF EXISTS idx_competitor_win_competitor;
DROP TABLE IF EXISTS competitor_win_records;
