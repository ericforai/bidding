CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    data_scope VARCHAR(32) NOT NULL DEFAULT 'self',
    menu_permissions TEXT,
    allowed_projects TEXT,
    allowed_depts TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO roles (code, name, description, is_system, enabled, data_scope, menu_permissions)
VALUES
    ('admin', '管理员', '系统管理员，拥有所有权限', TRUE, TRUE, 'all', 'all'),
    ('manager', '经理', '部门经理，可查看项目、知识库、资源与分析数据', TRUE, TRUE, 'dept', 'dashboard,bidding,project,knowledge,resource,analytics,settings'),
    ('staff', '员工', '业务人员，可查看工作台、标讯、项目、知识库与资源', TRUE, TRUE, 'self', 'dashboard,bidding,project,knowledge,resource');

ALTER TABLE users ADD COLUMN role_id BIGINT NULL;

UPDATE users
SET role_id = (
    SELECT id FROM roles WHERE code = LOWER(users.role)
    LIMIT 1
);

UPDATE users
SET role_id = (
    SELECT id FROM roles WHERE code = 'staff' LIMIT 1
)
WHERE role_id IS NULL;

ALTER TABLE users
    MODIFY COLUMN role_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_users_role_id FOREIGN KEY (role_id) REFERENCES roles(id);
