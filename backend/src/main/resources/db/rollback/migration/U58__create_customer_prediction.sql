-- Input: migration/V58__create_customer_prediction.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_cp_opportunity_score;
DROP INDEX IF EXISTS idx_cp_status;
DROP INDEX IF EXISTS idx_cp_purchaser_hash;
DROP TABLE IF EXISTS customer_predictions;
