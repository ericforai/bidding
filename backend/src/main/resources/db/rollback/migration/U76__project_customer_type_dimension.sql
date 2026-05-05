-- Input: migration/V76__project_customer_type_dimension.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_project_customer_type;
ALTER TABLE projects DROP COLUMN IF EXISTS customer_type;
