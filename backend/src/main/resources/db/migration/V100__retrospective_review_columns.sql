-- V100: retrospective 增补 result_type 与 review_comment（WS-E）
ALTER TABLE project_retrospective
    ADD COLUMN IF NOT EXISTS result_type VARCHAR(16);
ALTER TABLE project_retrospective
    ADD COLUMN IF NOT EXISTS review_comment VARCHAR(2048);
CREATE INDEX IF NOT EXISTS idx_retrospective_result_type ON project_retrospective(result_type);
