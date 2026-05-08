-- Input: migration/V67__add_template_classification_dimensions.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_templates_catalog_filter;
DROP INDEX IF EXISTS idx_templates_document_type;
DROP INDEX IF EXISTS idx_templates_industry;
DROP INDEX IF EXISTS idx_templates_product_type;
ALTER TABLE templates DROP COLUMN IF EXISTS document_type;
ALTER TABLE templates DROP COLUMN IF EXISTS industry;
ALTER TABLE templates DROP COLUMN IF EXISTS product_type;
