一旦我所属的文件夹有所变化，请更新我。

# API 模块目录

这里按业务域拆分前端 API module，是页面、组件、store 获取业务数据的唯一入口。
所有模块均只被允许向真实的后端微服务发起 HTTP 通信，不允许隐式绕行任何本地静态源兜底。

| 文件 | 地位 | 功能 |
|------|------|------|
| `ai.js` | API 模块 | AI 评分、竞争、ROI、合规等智能分析调用 |
| `approval.js` | API 模块 | 审批流和审批记录相关调用 |
| `audit.js` | API 模块 | 审计日志和系统审计相关调用 |
| `auth.js` | API 模块 | 登录、登出、当前用户和鉴权相关调用 |
| `bidProcess.js` | API 模块 | 标书编制流程启动、初稿上传、聚合详情调用 |
| `collaboration.js` | API 模块 | 协作线程、评论、版本、文档协同调用 |
| `customerOpportunity.js` | 特性 adapter 模块 | 客户机会中心的 demo 适配和未接入态治理 |
| `dashboard.js` | API 模块 | Dashboard 总览、统计、任务和日历调用 |
| `export.js` | API 模块 | 导出任务、导出状态和格式枚举 |
| `fees.js` | API 模块 | 费用申请、审批、退还等调用 |
| `knowledge.js` | API 模块 | 资质、案例、模板等知识资产调用 |
| `projectGroups.js` | API 模块 | 项目组正式领域模型的管理、删除与项目绑定配置 |
| `projects.js` | API 模块 | 项目列表、详情、任务、评分、结果录入调用 |
| `resources.js` | API 模块 | 平台账号、BAR、证书、资源能力调用 |
| `settings.js` | API 模块 | 系统设置页的数据权限与组织树读写 |
| `tenders.js` | API 模块 | 标讯列表、详情、入项和关联调用 |

`auth.js` 返回的用户快照会保留会话级权限字段，例如 `allowedProjectIds` 和 `allowedDepts`，供 store 和路由恢复使用。
