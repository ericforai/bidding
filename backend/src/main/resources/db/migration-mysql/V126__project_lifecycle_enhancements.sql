-- V126: 投标项目全生命周期增强（蓝图 V1.1 §4.3）
-- 注意：summary (project_result, V113 已加) 与 review_status (project_retrospective, V108 已加) 复用现有列

-- ============ project_initiation_details: 立项审批字段 ============
ALTER TABLE project_initiation_details
    ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN reviewed_by BIGINT,
    ADD COLUMN reviewed_at DATETIME,
    ADD COLUMN customer_info_json JSON,
    ADD COLUMN tender_document_id BIGINT,
    ADD COLUMN ai_risk_level VARCHAR(16);
CREATE INDEX idx_project_initiation_review_status ON project_initiation_details(review_status);

-- ============ project_evaluation: 评标文件 + notes NOT NULL ============
ALTER TABLE project_evaluation
    MODIFY COLUMN notes VARCHAR(2048) NOT NULL DEFAULT '',
    ADD COLUMN evaluation_files_json JSON;

-- ============ project_result: 多凭证文件 (summary 复用 V113 现有列) ============
ALTER TABLE project_result
    ADD COLUMN evidence_file_ids JSON;

-- ============ project_retrospective: 会议信息 + 报告附件 (review_status 复用 V108 现有列) ============
ALTER TABLE project_retrospective
    ADD COLUMN meeting_time DATETIME,
    ADD COLUMN meeting_type VARCHAR(16),
    ADD COLUMN participants VARCHAR(500),
    ADD COLUMN process_issues TEXT,
    ADD COLUMN report_attachment_id BIGINT;

-- ============ project_closure: 审核流程 + 项目总结 ============
ALTER TABLE project_closure
    ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN reviewed_by BIGINT,
    ADD COLUMN reviewed_at DATETIME,
    ADD COLUMN project_summary TEXT;
CREATE INDEX idx_project_closure_review_status ON project_closure(review_status);

-- ============ project_task: 驳回原因 ============
ALTER TABLE project_task
    ADD COLUMN review_comment TEXT;
