-- Input: migration/V63__add_recorded_name_to_competitor_win_records.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE competitor_win_records DROP COLUMN IF EXISTS recorded_name;
