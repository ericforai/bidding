-- V110: Add blueprint V1.1 required tender fields (contact system, personnel, project type, bid notice)
ALTER TABLE tenders
    ADD COLUMN project_type VARCHAR(20) NULL COMMENT '项目类型: INDUSTRIAL_ECOMMERCE/OFFICE/COMPREHENSIVE/CENTRALIZED/OTHER',
    ADD COLUMN contact_tel VARCHAR(50) NULL COMMENT '联系人1座机',
    ADD COLUMN contact_mail VARCHAR(100) NULL COMMENT '联系人1邮箱',
    ADD COLUMN contact_name2 VARCHAR(100) NULL COMMENT '联系人2姓名',
    ADD COLUMN contact_phone2 VARCHAR(50) NULL COMMENT '联系人2手机号',
    ADD COLUMN contact_tel2 VARCHAR(50) NULL COMMENT '联系人2座机',
    ADD COLUMN contact_mail2 VARCHAR(100) NULL COMMENT '联系人2邮箱',
    ADD COLUMN project_manager_id BIGINT NULL COMMENT '项目负责人ID',
    ADD COLUMN project_manager_name VARCHAR(100) NULL COMMENT '项目负责人姓名',
    ADD COLUMN bidding_person_id BIGINT NULL COMMENT '投标负责人ID',
    ADD COLUMN bidding_person_name VARCHAR(100) NULL COMMENT '投标负责人姓名',
    ADD COLUMN department VARCHAR(100) NULL COMMENT '项目部门',
    ADD COLUMN distributor_id BIGINT NULL COMMENT '分配人ID',
    ADD COLUMN distributor_name VARCHAR(100) NULL COMMENT '分配人姓名',
    ADD COLUMN creator_id BIGINT NULL COMMENT '创建人ID',
    ADD COLUMN creator_name VARCHAR(100) NULL COMMENT '创建人姓名',
    ADD COLUMN bid_notice TEXT NULL COMMENT '标讯信息原文',
    ADD COLUMN bid_notice_file_url VARCHAR(1000) NULL COMMENT '标讯文件URL';

CREATE INDEX idx_tender_project_type ON tenders(project_type);
CREATE INDEX idx_tender_project_manager ON tenders(project_manager_id);
CREATE INDEX idx_tender_bidding_person ON tenders(bidding_person_id);
