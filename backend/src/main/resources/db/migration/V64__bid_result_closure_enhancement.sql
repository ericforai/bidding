-- 投标结果闭环：为 bid_result_fetch_results 补齐实体中已声明的扩展字段
-- 所有 ADD COLUMN 采用 IF NOT EXISTS，兼容已有该列的老环境

ALTER TABLE bid_result_fetch_results ADD COLUMN IF NOT EXISTS registration_type VARCHAR(20);
ALTER TABLE bid_result_fetch_results ADD COLUMN IF NOT EXISTS contract_start_date DATE;
ALTER TABLE bid_result_fetch_results ADD COLUMN IF NOT EXISTS contract_end_date DATE;
ALTER TABLE bid_result_fetch_results ADD COLUMN IF NOT EXISTS contract_duration_months INTEGER;
ALTER TABLE bid_result_fetch_results ADD COLUMN IF NOT EXISTS remark VARCHAR(2000);
ALTER TABLE bid_result_fetch_results ADD COLUMN IF NOT EXISTS sku_count INTEGER;
ALTER TABLE bid_result_fetch_results ADD COLUMN IF NOT EXISTS win_announce_doc_url VARCHAR(500);
