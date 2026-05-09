-- U113 rollback for V113
ALTER TABLE project_result
    DROP COLUMN summary,
    DROP COLUMN evidence_doc_ids;
