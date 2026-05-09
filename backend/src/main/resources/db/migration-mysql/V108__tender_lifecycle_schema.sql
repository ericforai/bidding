-- V99: 投标项目 6 阶段全流程基础表 + projects.stage 列
-- PRD §3.1/§3.3/§3.4/§3.5/§3.6 + §5.4 FSM
-- 既有 projects.status 保留兼容；stage 为新阶段口径。

ALTER TABLE projects
    ADD COLUMN stage VARCHAR(32) NOT NULL DEFAULT 'INITIATED';

CREATE INDEX idx_projects_stage ON projects(stage);

-- §3.1 立项详情（1:1 扩展 projects，避免破坏 entity/Project.java）
CREATE TABLE IF NOT EXISTS project_initiation_details (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    bid_open_time TIMESTAMP,
    bid_month VARCHAR(16),
    expected_bidders INT,
    customer_type VARCHAR(64),
    annual_revenue DECIMAL(20, 2),
    contract_period_months INT,
    competitors VARCHAR(1024),
    department_snapshot VARCHAR(255),
    deposit_amount DECIMAL(20, 2),
    deposit_payment_method VARCHAR(64),
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_initiation_project UNIQUE (project_id)
);
CREATE INDEX idx_initiation_project ON project_initiation_details(project_id);

-- §3.3 评标
CREATE TABLE IF NOT EXISTS project_evaluation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    sub_stage VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    evaluation_started_at TIMESTAMP,
    board_received_at TIMESTAMP,
    announced_at TIMESTAMP,
    notes VARCHAR(2048),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_evaluation_project UNIQUE (project_id)
);
CREATE INDEX idx_evaluation_project ON project_evaluation(project_id);

-- §3.4 结果确认
CREATE TABLE IF NOT EXISTS project_result (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    result_type VARCHAR(16) NOT NULL,
    award_amount DECIMAL(20, 2),
    contract_start_date DATE,
    contract_end_date DATE,
    evidence_attachment_id BIGINT,
    registered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_result_project UNIQUE (project_id)
);
CREATE INDEX idx_result_project ON project_result(project_id);
CREATE INDEX idx_result_type ON project_result(result_type);

-- §3.5 复盘
CREATE TABLE IF NOT EXISTS project_retrospective (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    summary VARCHAR(4000),
    win_factors VARCHAR(2048),
    loss_reasons VARCHAR(2048),
    competitor_notes VARCHAR(2048),
    improvement_actions VARCHAR(2048),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_retrospective_project UNIQUE (project_id)
);
CREATE INDEX idx_retrospective_project ON project_retrospective(project_id);

-- §3.6 结项（含 stage_locked 全字段锁定标记）
CREATE TABLE IF NOT EXISTS project_closure (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    closed_at TIMESTAMP,
    closed_by BIGINT,
    deposit_returned BOOLEAN NOT NULL DEFAULT FALSE,
    deposit_return_evidence_id BIGINT,
    archive_location VARCHAR(512),
    stage_locked BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(2048),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_closure_project UNIQUE (project_id)
);
CREATE INDEX idx_closure_project ON project_closure(project_id);
CREATE INDEX idx_closure_locked ON project_closure(stage_locked);
