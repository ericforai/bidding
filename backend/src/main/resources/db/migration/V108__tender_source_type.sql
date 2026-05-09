-- 添加标讯来源类型字段
-- MANUAL: 人工录入
-- EXTERNAL: 外部获取
ALTER TABLE tenders ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) DEFAULT 'MANUAL' COMMENT '标讯来源类型: MANUAL-人工录入, EXTERNAL-外部获取';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_tender_source_type ON tenders (source_type);

-- 更新现有数据：source 字段为 'manual' 的设为 MANUAL，其他设为 EXTERNAL
UPDATE tenders SET source_type = 'MANUAL' WHERE source = 'manual';
UPDATE tenders SET source_type = 'EXTERNAL' WHERE (source_type IS NULL OR source_type = '') AND source != 'manual';
