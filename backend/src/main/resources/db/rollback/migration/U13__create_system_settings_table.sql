-- Input: migration/V13__create_system_settings_table.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_system_settings_config_key;
DROP TABLE IF EXISTS system_settings;
