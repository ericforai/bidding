CREATE TABLE IF NOT EXISTS business_qualifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    subject_type VARCHAR(32) NOT NULL,
    subject_name VARCHAR(200) NOT NULL,
    category VARCHAR(32) NOT NULL,
    certificate_no VARCHAR(120),
    issuer VARCHAR(200),
    holder_name VARCHAR(120),
    issue_date DATE,
    expiry_date DATE,
    status VARCHAR(32) NOT NULL,
    reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_days INT NOT NULL DEFAULT 30,
    last_reminded_at TIMESTAMP NULL,
    current_borrow_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    current_borrower VARCHAR(120),
    current_department VARCHAR(120),
    current_project_id VARCHAR(64),
    borrow_purpose VARCHAR(255),
    expected_return_date DATE,
    file_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS qualification_loan_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    qualification_id BIGINT NOT NULL,
    borrower VARCHAR(120) NOT NULL,
    department VARCHAR(120),
    project_id VARCHAR(64),
    purpose VARCHAR(255),
    remark VARCHAR(500),
    borrowed_at TIMESTAMP NOT NULL,
    expected_return_date DATE,
    returned_at TIMESTAMP NULL,
    return_remark VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_qualification_loan_records_qualification
        FOREIGN KEY (qualification_id) REFERENCES business_qualifications(id)
);

CREATE TABLE IF NOT EXISTS qualification_attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    qualification_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_qualification_attachments_qualification
        FOREIGN KEY (qualification_id) REFERENCES business_qualifications(id)
);

CREATE INDEX idx_business_qualifications_expiry_date
    ON business_qualifications(expiry_date);

CREATE INDEX idx_business_qualifications_subject
    ON business_qualifications(subject_type, subject_name);

CREATE INDEX idx_qualification_loan_records_qualification_id
    ON qualification_loan_records(qualification_id);

CREATE INDEX idx_qualification_attachments_qualification_id
    ON qualification_attachments(qualification_id);
