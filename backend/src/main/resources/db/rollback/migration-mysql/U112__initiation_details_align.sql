-- U112 rollback for V112
ALTER TABLE project_initiation_details
    DROP COLUMN owner_unit,
    DROP COLUMN project_type,
    DROP COLUMN owner_user_id;
