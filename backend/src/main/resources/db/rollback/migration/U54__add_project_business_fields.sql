-- Input: migration/V54__add_project_business_fields.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE projects DROP COLUMN IF EXISTS tags_json;
ALTER TABLE projects DROP COLUMN IF EXISTS remark;
ALTER TABLE projects DROP COLUMN IF EXISTS description;
ALTER TABLE projects DROP COLUMN IF EXISTS deadline;
ALTER TABLE projects DROP COLUMN IF EXISTS platform;
ALTER TABLE projects DROP COLUMN IF EXISTS region;
ALTER TABLE projects DROP COLUMN IF EXISTS industry;
ALTER TABLE projects DROP COLUMN IF EXISTS budget;
ALTER TABLE projects DROP COLUMN IF EXISTS customer;
