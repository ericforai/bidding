-- Input: migration/V85__bid_match_scoring.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_bid_match_eval_time;
DROP INDEX IF EXISTS idx_bid_match_eval_version;
DROP INDEX IF EXISTS idx_bid_match_eval_tender;
DROP TABLE IF EXISTS bid_match_score_evaluations;
DROP INDEX IF EXISTS uk_bid_match_version_model_no;
DROP INDEX IF EXISTS idx_bid_match_version_active;
DROP INDEX IF EXISTS idx_bid_match_version_model;
DROP TABLE IF EXISTS bid_match_model_versions;
DROP INDEX IF EXISTS idx_bid_match_model_status;
DROP TABLE IF EXISTS bid_match_scoring_models;
