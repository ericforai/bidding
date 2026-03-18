> 一旦我所属的文件夹有所变化，请更新我。

# Audit 模块

审计模块负责审计日志查询结果的传输对象，服务管理端日志检索和汇总展示。
该目录只承载审计日志的读模型与汇总返回，不负责日志采集。
对外输出用于列表、查询和统计展示的 DTO。

| 文件 | 地位 | 功能 |
|------|------|------|
| `dto/` | 子目录 | 审计日志 DTO 边界 |
| `dto/AuditLogItemDTO.java` | DTO | 审计日志明细项 |
| `dto/AuditLogQueryResponse.java` | DTO | 审计日志查询响应 |
| `dto/AuditLogSummaryDTO.java` | DTO | 审计日志汇总统计 |
