-- Input: migration/V65__bid_result_attachment_closure_fields.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

ALTER TABLE project_documents DROP COLUMN IF EXISTS file_url;
ALTER TABLE project_documents DROP COLUMN IF EXISTS linked_entity_id;
ALTER TABLE project_documents DROP COLUMN IF EXISTS linked_entity_type;
ALTER TABLE project_documents DROP COLUMN IF EXISTS document_category;
ALTER TABLE bid_result_reminders DROP COLUMN IF EXISTS uploaded_by;
ALTER TABLE bid_result_reminders DROP COLUMN IF EXISTS uploaded_at;
ALTER TABLE bid_result_reminders DROP COLUMN IF EXISTS attachment_document_id;
ALTER TABLE bid_result_fetch_results DROP COLUMN IF EXISTS analysis_document_id;
ALTER TABLE bid_result_fetch_results DROP COLUMN IF EXISTS notice_document_id;
