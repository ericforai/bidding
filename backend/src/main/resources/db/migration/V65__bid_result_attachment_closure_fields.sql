ALTER TABLE bid_result_fetch_results ADD COLUMN IF NOT EXISTS notice_document_id BIGINT;
ALTER TABLE bid_result_fetch_results ADD COLUMN IF NOT EXISTS analysis_document_id BIGINT;

ALTER TABLE bid_result_reminders ADD COLUMN IF NOT EXISTS attachment_document_id BIGINT;
ALTER TABLE bid_result_reminders ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP;
ALTER TABLE bid_result_reminders ADD COLUMN IF NOT EXISTS uploaded_by BIGINT;

ALTER TABLE project_documents ADD COLUMN IF NOT EXISTS document_category VARCHAR(64);
ALTER TABLE project_documents ADD COLUMN IF NOT EXISTS linked_entity_type VARCHAR(64);
ALTER TABLE project_documents ADD COLUMN IF NOT EXISTS linked_entity_id BIGINT;
ALTER TABLE project_documents ADD COLUMN IF NOT EXISTS file_url VARCHAR(1000);
