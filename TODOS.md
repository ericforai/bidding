# TODOs

## 后端数据权限范围查询下推

- **What**: 将统计与导出的可见项目范围计算从 `projectRepository.findAll()` 后内存过滤，下推为数据库层的当前用户可见项目 ID 查询。
- **Why**: 当前实现安全上会过滤不可见项目，但普通用户访问统计、客户类型分析或导出时仍会先加载全量项目；项目量增长后会放大延迟和内存占用。
- **Pros**: 降低非管理员统计和导出的查询成本，避免大租户/大项目量下出现慢接口，同时让权限范围语义更集中。
- **Cons**: 需要扩展 `ProjectAccessScopeService` 或 `ProjectRepository` 的范围查询接口，并补充管理员、个人、部门、显式项目、项目组授权等多种数据范围测试。
- **Context**: P1 修复已在 `DashboardAnalyticsQueryService`、`CustomerTypeAnalyticsQueryService` 和 `ExcelExportService` 中复用 `ProjectAccessScopeService`，完成安全收口；下一步应把 `filterAccessibleProjects(projectRepository.findAll())` 替换为可复用的 `currentUserAccessibleProjectIds()` 一类查询入口。
- **Depends on / blocked by**: 需要保持现有 `ProjectAccessScopeService` 作为唯一权限体系，不新增并行权限模型；建议在性能专项或 P2/P3 技术债任务中处理。

## QA Deferred: 非管理员演示账号不可登录

- **Severity**: Medium
- **Category**: UX / testability
- **Found by**: `/qa` on `codex/p1-stats-export-ai-access`, 2026-04-25
- **Repro**: 登录页展示 `lizong`、`zhangjingli`、`xiaowang` 等演示账号，但使用公开文档中出现的 `123456` 或常见演示密码登录均返回 401。
- **Impact**: 浏览器 QA 无法直接用演示普通用户账号验证非管理员数据范围；当前只能依赖后端集成测试验证普通用户隔离。
- **Suggested fix**: 明确移除不可用账号提示，或在本地/e2e profile 中种子化这些账号并公开对应本地演示密码。

## QA Deferred: 导出响应 recordCount 始终为 0

- **Severity**: Low
- **Category**: Functional
- **Found by**: `/qa` on `codex/p1-stats-export-ai-access`, 2026-04-25
- **Repro**: 管理员调用 `POST /api/export/excel`，请求 `{"dataType":"tenders","async":false,"params":{}}` 返回 200 且生成 xlsx，但响应 `data.recordCount` 为 0；同环境 `/api/tenders` 返回 116 条。
- **Impact**: 导出文件生成成功，但前端或调用方若展示导出记录数会得到误导性结果。
- **Suggested fix**: 让导出服务返回文件大小与真实记录数的结构化结果，或在控制器层从服务返回值中读取真实记录数，避免继续使用占位的 `extractRecordCountFromPath()`。
