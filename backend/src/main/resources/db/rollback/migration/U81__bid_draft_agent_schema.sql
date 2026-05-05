-- Input: migration/V81__bid_draft_agent_schema.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

DROP INDEX IF EXISTS idx_bid_agent_artifacts_status;
DROP INDEX IF EXISTS idx_bid_agent_artifacts_type;
DROP INDEX IF EXISTS idx_bid_agent_artifacts_run;
DROP TABLE IF EXISTS bid_agent_artifacts;
DROP INDEX IF EXISTS idx_bid_agent_runs_status;
DROP INDEX IF EXISTS idx_bid_agent_runs_tender;
DROP INDEX IF EXISTS idx_bid_agent_runs_project;
DROP TABLE IF EXISTS bid_agent_runs;
