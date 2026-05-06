-- Input: migration/V9__project_source_metadata.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE projects DROP COLUMN IF EXISTS source_reasoning_summary;
ALTER TABLE projects DROP COLUMN IF EXISTS source_opportunity_id;
ALTER TABLE projects DROP COLUMN IF EXISTS source_customer;
ALTER TABLE projects DROP COLUMN IF EXISTS source_customer_id;
ALTER TABLE projects DROP COLUMN IF EXISTS source_module;
