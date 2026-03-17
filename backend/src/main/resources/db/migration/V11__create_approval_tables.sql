-- 审批请求表
CREATE TABLE approval_requests (
    id UUID PRIMARY KEY,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(200),
    approval_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requester_id BIGINT NOT NULL,
    requester_name VARCHAR(100) NOT NULL,
    current_approver_id BIGINT,
    current_approver_name VARCHAR(100),
    priority INT NOT NULL DEFAULT 0,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    attachment_ids TEXT,
    submitted_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    due_date TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

-- 审批操作记录表
CREATE TABLE approval_actions (
    id UUID PRIMARY KEY,
    approval_request_id UUID NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    actor_id BIGINT NOT NULL,
    actor_name VARCHAR(100) NOT NULL,
    comment TEXT,
    action_time TIMESTAMP NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX idx_ar_project_id ON approval_requests(project_id);
CREATE INDEX idx_ar_status ON approval_requests(status);
CREATE INDEX idx_ar_requester_id ON approval_requests(requester_id);
CREATE INDEX idx_ar_created_at ON approval_requests(created_at);
CREATE INDEX idx_ar_approval_type ON approval_requests(approval_type);
CREATE INDEX idx_ar_current_approver ON approval_requests(current_approver_id);

CREATE INDEX idx_aa_approval_request_id ON approval_actions(approval_request_id);
CREATE INDEX idx_aa_action_time ON approval_actions(action_time);
CREATE INDEX idx_aa_actor_id ON approval_actions(actor_id);

-- 添加注释
COMMENT ON TABLE approval_requests IS '审批请求表';
COMMENT ON COLUMN approval_requests.id IS '唯一标识';
COMMENT ON COLUMN approval_requests.project_id IS '关联的项目ID';
COMMENT ON COLUMN approval_requests.approval_type IS '审批类型';
COMMENT ON COLUMN approval_requests.status IS '审批状态: PENDING, APPROVED, REJECTED, CANCELLED';
COMMENT ON COLUMN approval_requests.priority IS '优先级: 0-普通, 1-紧急, 2-非常紧急';

COMMENT ON TABLE approval_actions IS '审批操作记录表';
COMMENT ON COLUMN approval_actions.approval_request_id IS '关联的审批请求ID';
COMMENT ON COLUMN approval_actions.action_type IS '操作类型: SUBMIT, APPROVE, REJECT, CANCEL';
