# TODOs

## 后端数据权限范围查询下推

- **What**: 将统计与导出的可见项目范围计算从 `projectRepository.findAll()` 后内存过滤，下推为数据库层的当前用户可见项目 ID 查询。
- **Why**: 当前实现安全上会过滤不可见项目，但普通用户访问统计、客户类型分析或导出时仍会先加载全量项目；项目量增长后会放大延迟和内存占用。
- **Pros**: 降低非管理员统计和导出的查询成本，避免大租户/大项目量下出现慢接口，同时让权限范围语义更集中。
- **Cons**: 需要扩展 `ProjectAccessScopeService` 或 `ProjectRepository` 的范围查询接口，并补充管理员、个人、部门、显式项目、项目组授权等多种数据范围测试。
- **Context**: P1 修复已在 `DashboardAnalyticsQueryService`、`CustomerTypeAnalyticsQueryService` 和 `ExcelExportService` 中复用 `ProjectAccessScopeService`，完成安全收口；下一步应把 `filterAccessibleProjects(projectRepository.findAll())` 替换为可复用的 `currentUserAccessibleProjectIds()` 一类查询入口。
- **Depends on / blocked by**: 需要保持现有 `ProjectAccessScopeService` 作为唯一权限体系，不新增并行权限模型；建议在性能专项或 P2/P3 技术债任务中处理。
