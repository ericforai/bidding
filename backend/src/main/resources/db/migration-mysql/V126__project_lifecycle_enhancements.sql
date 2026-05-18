ALTER TABLE project_initiation_details
    ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN reviewed_by BIGINT,
    ADD COLUMN reviewed_at DATETIME,
    ADD COLUMN customer_info_json JSON,
    ADD COLUMN tender_document_id BIGINT,
    ADD COLUMN ai_risk_level VARCHAR(16);
CREATE INDEX idx_project_initiation_review_status ON project_initiation_details(review_status);

ALTER TABLE project_evaluation
    MODIFY COLUMN notes TEXT NOT NULL,
    ADD COLUMN evaluation_files_json JSON;

ALTER TABLE project_result
    ADD COLUMN evidence_file_ids JSON,
    ADD COLUMN summary TEXT;

ALTER TABLE project_retrospective
    ADD COLUMN meeting_time DATETIME,
    ADD COLUMN meeting_type VARCHAR(16),
    ADD COLUMN participants VARCHAR(500),
    ADD COLUMN process_issues TEXT,
    ADD COLUMN report_attachment_id BIGINT,
    ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE project_closure
    ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN reviewed_by BIGINT,
    ADD COLUMN reviewed_at DATETIME,
    ADD COLUMN project_summary TEXT;
CREATE INDEX idx_project_closure_review_status ON project_closure(review_status);

ALTER TABLE project_task
    ADD COLUMN review_comment TEXT;
