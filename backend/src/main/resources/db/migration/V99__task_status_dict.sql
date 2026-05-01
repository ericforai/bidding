-- V99: 任务状态字典（全平台统一主数据），解耦任务状态的显示与业务判断
CREATE TABLE task_status_dict (
    code         VARCHAR(32)  NOT NULL PRIMARY KEY,
    name         VARCHAR(64)  NOT NULL,
    category     VARCHAR(16)  NOT NULL,
    color        VARCHAR(16)  NOT NULL DEFAULT '#909399',
    sort_order   INT          NOT NULL DEFAULT 0,
    is_initial   TINYINT(1)   NOT NULL DEFAULT 0,
    is_terminal  TINYINT(1)   NOT NULL DEFAULT 0,
    enabled      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   VARCHAR(64)  NULL,
    CONSTRAINT ck_task_status_dict_category CHECK (category IN ('OPEN','IN_PROGRESS','REVIEW','CLOSED'))
);

CREATE INDEX idx_task_status_dict_enabled_sort ON task_status_dict (enabled, sort_order);

-- 种子：保持与历史 ENUM 等价的 4 条记录，确保业务语义一致
INSERT INTO task_status_dict (code, name, category, color, sort_order, is_initial, is_terminal, enabled)
VALUES
    ('TODO',        '待办',   'OPEN',        '#909399', 10, 1, 0, 1),
    ('IN_PROGRESS', '进行中', 'IN_PROGRESS', '#409eff', 20, 0, 0, 1),
    ('REVIEW',      '待审核', 'REVIEW',      '#e6a23c', 30, 0, 0, 1),
    ('COMPLETED',   '已完成', 'CLOSED',      '#67c23a', 40, 0, 1, 1);
