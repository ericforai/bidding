-- Input: migration-mysql/V125__project_lifecycle_enhancements.sql
-- Output: rollback script for mysql environments; removes V125 project lifecycle enhancements.
-- Pos: Flyway down migration coverage for 西域数智化投标管理平台.

-- ============ project_task ============
ALTER TABLE project_task DROP COLUMN review_comment;

-- ============ project_closure ============
DROP INDEX idx_project_closure_review_status ON project_closure;
ALTER TABLE project_closure
    DROP COLUMN project_summary,
    DROP COLUMN reviewed_at,
    DROP COLUMN reviewed_by,
    DROP COLUMN review_status;

-- ============ project_retrospective ============
ALTER TABLE project_retrospective
    DROP COLUMN review_status,
    DROP COLUMN report_attachment_id,
    DROP COLUMN process_issues,
    DROP COLUMN participants,
    DROP COLUMN meeting_type,
    DROP COLUMN meeting_time;

-- ============ project_result ============
ALTER TABLE project_result
    DROP COLUMN summary,
    DROP COLUMN evidence_file_ids;

-- ============ project_evaluation ============
-- Restore notes nullability (was NOT NULL after V125)
ALTER TABLE project_evaluation
    MODIFY COLUMN notes TEXT NULL,
    DROP COLUMN evaluation_files_json;

-- ============ project_initiation_details ============
DROP INDEX idx_project_initiation_review_status ON project_initiation_details;
ALTER TABLE project_initiation_details
    DROP COLUMN ai_risk_level,
    DROP COLUMN tender_document_id,
    DROP COLUMN customer_info_json,
    DROP COLUMN reviewed_at,
    DROP COLUMN reviewed_by,
    DROP COLUMN rejection_reason,
    DROP COLUMN review_status;
