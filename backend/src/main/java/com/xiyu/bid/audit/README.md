> 一旦我所属的文件夹有所变化，请更新我。

# 操作日志模块

操作日志模块负责关键操作记录的写入、查询和管理员端展示。
底层仍沿用历史 `audit_logs` 表、`/api/audit` 路径与 `audit` 包名，避免引入迁移风险；面向用户统一表达为“操作日志”或“关键操作记录”。
纯核心负责判断哪些 action 属于关键操作；应用服务负责上下文采集、异步写入、查询编排和 DTO 转换。
第一版只记录新增、修改、删除和状态流转类关键动作，查询、浏览、列表、搜索等动作在写入源头跳过。

| 文件 | 地位 | 功能 |
|------|------|------|
| `core/` | 子目录 | 纯核心策略 |
| `core/AuditActionPolicy.java` | Core | 判断操作是否需要记录 |
| `service/` | 子目录 | 操作日志应用服务 |
| `service/AuditLogService.java` | Service | 操作日志 facade，保留原有契约 |
| `service/AuditLogWriter.java` | Service | 操作日志写入编排 |
| `service/AuditLogQueryService.java` | Service | 操作日志查询编排 |
| `service/AuditLogItemMapper.java` | Mapper | Entity/User 到列表 DTO 转换 |
| `service/AuditLogMapper.java` | Mapper | 事件命令到实体转换 |
| `service/AuditRequestContextProvider.java` | Adapter | 请求 IP 与 User-Agent 采集 |
| `dto/` | 子目录 | 操作日志 DTO 边界 |
| `dto/AuditLogItemDTO.java` | DTO | 操作日志明细项 |
| `dto/AuditLogQueryResponse.java` | DTO | 操作日志查询响应 |
| `dto/AuditLogSummaryDTO.java` | DTO | 操作日志汇总统计 |
