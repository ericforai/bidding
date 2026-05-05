-- Input: migration/V4__bar_site_subresources.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_bar_site_verifications_asset_id;
DROP TABLE IF EXISTS bar_site_verifications;
DROP INDEX IF EXISTS idx_bar_site_attachments_asset_id;
DROP TABLE IF EXISTS bar_site_attachments;
DROP TABLE IF EXISTS bar_site_sops;
DROP INDEX IF EXISTS idx_bar_site_accounts_asset_id;
DROP TABLE IF EXISTS bar_site_accounts;
