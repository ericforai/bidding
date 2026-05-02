-- V102: Task 新增 content 富文本字段；status 列确保 VARCHAR(32)
--       历史数据 TODO/IN_PROGRESS/REVIEW/COMPLETED/CANCELLED 已与 task_status_dict.code
--       一致（V101 种子前 4 条；CANCELLED 不在字典里但不破坏存储）。
--       字典 code 外键约束暂不加硬 FK，避免和未来"字典管理页"禁用某状态时的
--       历史数据冲突；由 service 层 + 架构测试守卫。

ALTER TABLE tasks
    ADD COLUMN content MEDIUMTEXT NULL COMMENT '任务详细描述（Markdown 文本）';

ALTER TABLE tasks MODIFY COLUMN status VARCHAR(32) NOT NULL;
