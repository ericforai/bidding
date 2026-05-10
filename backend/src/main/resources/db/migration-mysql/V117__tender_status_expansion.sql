-- 标讯状态扩展与评分分析关联增强

-- 1. 更新已有的标讯状态
UPDATE tenders SET status = 'PENDING_ASSIGNMENT' WHERE status = 'PENDING';
-- 将已投标状态映射到投标中，或根据业务理解保持 BIDDED -> BIDDING
UPDATE tenders SET status = 'BIDDING' WHERE status = 'BIDDED';

-- 2. 增强评分分析表以支持标讯
ALTER TABLE score_analyses MODIFY project_id BIGINT NULL;
ALTER TABLE score_analyses ADD COLUMN tender_id BIGINT NULL AFTER project_id;
ALTER TABLE score_analyses ADD INDEX idx_score_analysis_tender (tender_id);
