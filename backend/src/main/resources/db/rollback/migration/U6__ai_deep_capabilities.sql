-- Input: migration/V6__ai_deep_capabilities.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE project_score_previews DROP CONSTRAINT IF EXISTS fk_score_preview_tender;
ALTER TABLE project_score_previews DROP CONSTRAINT IF EXISTS fk_score_preview_project;
ALTER TABLE ai_analysis_results DROP CONSTRAINT IF EXISTS fk_ai_result_project;
ALTER TABLE ai_analysis_results DROP CONSTRAINT IF EXISTS fk_ai_result_tender;
ALTER TABLE ai_analysis_results DROP CONSTRAINT IF EXISTS fk_ai_result_job;
DROP INDEX IF EXISTS idx_score_preview_tender;
DROP INDEX IF EXISTS idx_score_preview_project;
DROP INDEX IF EXISTS idx_ai_result_job;
DROP INDEX IF EXISTS idx_ai_result_project;
DROP INDEX IF EXISTS idx_ai_result_tender;
DROP INDEX IF EXISTS idx_ai_job_status;
DROP INDEX IF EXISTS idx_ai_job_target;
DROP TABLE IF EXISTS project_score_previews;
DROP TABLE IF EXISTS ai_analysis_results;
DROP TABLE IF EXISTS ai_analysis_jobs;
