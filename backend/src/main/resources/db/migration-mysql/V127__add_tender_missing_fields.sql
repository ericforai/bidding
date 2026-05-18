-- V127: 补齐 tenders 表中 Tender 实体预期的剩余缺失字段
-- V75/V78 已补齐大部分基础字段（region, industry, purchaser_hash, contact_name/phone,
-- 归一化字段等），V126 已补齐 bid_notice。本迁移补齐从 H2 V110 中尚未
-- 在 MySQL 侧覆盖的字段：联系人2信息、项目/标讯负责人员信息。

ALTER TABLE tenders
    ADD COLUMN contact_tel VARCHAR(50) NULL COMMENT '联系人1座机',
    ADD COLUMN contact_mail VARCHAR(100) NULL COMMENT '联系人1邮箱',
    ADD COLUMN contact_name2 VARCHAR(100) NULL COMMENT '联系人2姓名',
    ADD COLUMN contact_phone2 VARCHAR(50) NULL COMMENT '联系人2手机号',
    ADD COLUMN contact_tel2 VARCHAR(50) NULL COMMENT '联系人2座机',
    ADD COLUMN contact_mail2 VARCHAR(100) NULL COMMENT '联系人2邮箱',
    ADD COLUMN project_type VARCHAR(20) NULL COMMENT '项目类型',
    ADD COLUMN project_manager_id BIGINT NULL COMMENT '项目负责人ID',
    ADD COLUMN project_manager_name VARCHAR(100) NULL COMMENT '项目负责人姓名',
    ADD COLUMN bidding_person_id BIGINT NULL COMMENT '投标负责人ID',
    ADD COLUMN bidding_person_name VARCHAR(100) NULL COMMENT '投标负责人姓名',
    ADD COLUMN department VARCHAR(100) NULL COMMENT '部门',
    ADD COLUMN distributor_id BIGINT NULL COMMENT '分配人ID',
    ADD COLUMN distributor_name VARCHAR(100) NULL COMMENT '分配人姓名',
    ADD COLUMN creator_id BIGINT NULL COMMENT '创建人ID',
    ADD COLUMN creator_name VARCHAR(100) NULL COMMENT '创建人姓名';
