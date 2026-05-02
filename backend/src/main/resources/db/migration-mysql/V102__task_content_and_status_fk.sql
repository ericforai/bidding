-- V102: Task 新增 content 富文本字段；status 列由 ENUM 改为 VARCHAR(32)
-- 设计说明：
--   1) 不加到 task_status_dict.code 的硬 FK。原因：未来字典管理页禁用某状态时，
--      历史 tasks 行仍引用该 code；service 层 + ArchitectureTest 负责守卫。
--   2) 历史值 TODO/IN_PROGRESS/REVIEW/COMPLETED 对应 V101 种子；CANCELLED 不在
--      字典中，列类型切换后仍以字符串 'CANCELLED' 保留在表中，未来由字典管理
--      页或数据修复脚本统一处理（保留、补登字典或归档），当前 V102 不做回填。
--   3) 新增 content 用 TEXT（64KB）而非 MEDIUMTEXT；Markdown 任务描述 64KB 绰绰
--      有余，应用层再加长度校验即可。

ALTER TABLE tasks
    ADD COLUMN content TEXT NULL COMMENT '任务详细描述（Markdown 文本，上限 64KB）';

ALTER TABLE tasks MODIFY COLUMN status VARCHAR(32) NOT NULL;

-- 支撑 TaskRepository 中的 findByStatus / findByAssigneeIdAndStatus / ...
CREATE INDEX idx_tasks_status ON tasks (status);
-- 支撑更频繁的 findByProjectIdAndStatus / countByProjectIdAndStatus / findByProjectIdAndStatusIn
CREATE INDEX idx_tasks_project_status ON tasks (project_id, status);
