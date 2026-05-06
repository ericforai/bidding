-- Input: migration/V15__add_user_department_columns.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE users DROP COLUMN IF EXISTS department_name;
ALTER TABLE users DROP COLUMN IF EXISTS department_code;
