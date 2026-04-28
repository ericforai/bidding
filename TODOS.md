# TODOS

## 拆分项目级招标文件拆解模块命名

- **What**: 将项目级招标文件拆解入口从 `biddraftagent` 包名下迁移到更中性的 `projecttenderbreakdown` 或 `projectworkflow` 模块边界。
- **Why**: 当前解析能力已经同时服务“拆解任务”和“AI 生成初稿”，继续放在 `biddraftagent` 下容易让后续维护者误以为它只属于初稿生成。
- **Pros**: 降低产品路径和代码边界漂移风险，让“解析招标文件 -> 任务拆解/初稿生成复用”的数据流更直观。
- **Cons**: 需要小范围移动 controller/facade/service 命名，并同步测试、文档和可能的架构门禁基线。
- **Context**: 本次 PR 先复用既有 `BidTenderDocumentImportAppService.parseTenderDocument()` 闭环，避免把可验证功能扩大成模块迁移；后续做迁移时应保留现有 API 路径 `/api/projects/{projectId}/tender-breakdown` 不变。
- **Depends on / blocked by**: 当前独立解析入口上线并稳定后执行，避免和任务拆解修复混在同一个变更里。
