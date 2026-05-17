-- V126: 补齐 tenders 表中 Tender 实体预期的缺失字段
-- 这些字段来自 Tender.java 实体定义，因 MySQL 基线（B73）和增量迁移均未覆盖
-- 对应 H2 迁移 V110__tender_full_fields.sql 中的字段已部分由 V125 补齐，
-- 剩余字段及实体中其他未命中的字段在此统一对齐

ALTER TABLE tenders
    -- 基础信息字段
    ADD COLUMN region VARCHAR(100) NULL COMMENT '区域',
    ADD COLUMN industry VARCHAR(100) NULL COMMENT '行业',
    ADD COLUMN purchaser_name VARCHAR(255) NULL COMMENT '采购人名称',
    ADD COLUMN purchaser_hash VARCHAR(64) NULL COMMENT '采购人哈希',
    -- 归一化搜索字段
    ADD COLUMN source_normalized VARCHAR(200) NULL COMMENT '归一化来源',
    ADD COLUMN region_normalized VARCHAR(100) NULL COMMENT '归一化区域',
    ADD COLUMN industry_normalized VARCHAR(100) NULL COMMENT '归一化行业',
    ADD COLUMN purchaser_hash_normalized VARCHAR(64) NULL COMMENT '归一化采购人哈希',
    ADD COLUMN purchaser_name_normalized VARCHAR(255) NULL COMMENT '归一化采购人名称',
    ADD COLUMN search_text_normalized TEXT NULL COMMENT '归一化搜索文本',
    -- 发布日期
    ADD COLUMN publish_date DATE NULL COMMENT '发布日期',
    -- 联系人信息
    ADD COLUMN contact_name VARCHAR(100) NULL COMMENT '联系人1姓名',
    ADD COLUMN contact_phone VARCHAR(50) NULL COMMENT '联系人1手机号',
    ADD COLUMN contact_tel VARCHAR(50) NULL COMMENT '联系人1座机',
    ADD COLUMN contact_mail VARCHAR(100) NULL COMMENT '联系人1邮箱',
    ADD COLUMN contact_name2 VARCHAR(100) NULL COMMENT '联系人2姓名',
    ADD COLUMN contact_phone2 VARCHAR(50) NULL COMMENT '联系人2手机号',
    ADD COLUMN contact_tel2 VARCHAR(50) NULL COMMENT '联系人2座机',
    ADD COLUMN contact_mail2 VARCHAR(100) NULL COMMENT '联系人2邮箱',
    -- 项目信息
    ADD COLUMN project_type VARCHAR(20) NULL COMMENT '项目类型',
    ADD COLUMN project_manager_id BIGINT NULL COMMENT '项目负责人ID',
    ADD COLUMN project_manager_name VARCHAR(100) NULL COMMENT '项目负责人姓名',
    ADD COLUMN bidding_person_id BIGINT NULL COMMENT '投标负责人ID',
    ADD COLUMN bidding_person_name VARCHAR(100) NULL COMMENT '投标负责人姓名',
    ADD COLUMN department VARCHAR(100) NULL COMMENT '部门',
    -- 分配与创建信息
    ADD COLUMN distributor_id BIGINT NULL COMMENT '分配人ID',
    ADD COLUMN distributor_name VARCHAR(100) NULL COMMENT '分配人姓名',
    ADD COLUMN creator_id BIGINT NULL COMMENT '创建人ID',
    ADD COLUMN creator_name VARCHAR(100) NULL COMMENT '创建人姓名',
    -- 通用文本字段
    ADD COLUMN description TEXT NULL COMMENT '描述',
    ADD COLUMN tags TEXT NULL COMMENT '标签';
