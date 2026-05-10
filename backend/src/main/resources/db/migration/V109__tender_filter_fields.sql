-- V108__tender_filter_fields.sql
-- 为标讯中心增加筛选条件：报名截止时间、开标时间、客户类型、客户级别
-- 添加报名截止时间字段用于筛选

ALTER TABLE tenders ADD COLUMN IF NOT EXISTS registration_deadline TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_tender_registration_deadline ON tenders(registration_deadline);
CREATE INDEX IF NOT EXISTS idx_tender_bid_opening_time ON tenders(bid_opening_time);
