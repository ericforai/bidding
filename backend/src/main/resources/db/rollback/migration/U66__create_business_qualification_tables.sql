-- Input: migration/V66__create_business_qualification_tables.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_qualification_attachments_qualification_id;
DROP INDEX IF EXISTS idx_qualification_loan_records_qualification_id;
DROP INDEX IF EXISTS idx_business_qualifications_subject;
DROP INDEX IF EXISTS idx_business_qualifications_expiry_date;
DROP TABLE IF EXISTS qualification_attachments;
DROP TABLE IF EXISTS qualification_loan_records;
DROP TABLE IF EXISTS business_qualifications;
