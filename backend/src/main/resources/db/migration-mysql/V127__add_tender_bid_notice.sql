-- 补充标讯通知公告字段
-- bid_notice / bid_notice_file_url 在 Tender 实体中已定义
-- 对应的 V110 迁移放在错误目录，此处补齐

ALTER TABLE tenders
    ADD COLUMN bid_notice TEXT NULL COMMENT '公告正文' AFTER bid_opening_time;
ALTER TABLE tenders
    ADD COLUMN bid_notice_file_url VARCHAR(1000) NULL COMMENT '公告附件 URL' AFTER bid_notice;
