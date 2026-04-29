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
| `bidAgent.js` | API 模块 | 项目标书写作 Agent 运行、状态、写入和审查调用 |
| `bidMatchScoring.js` | API 模块 | 投标匹配评分模型、模型激活和标讯评分结果调用 |
| `collaboration.js` | API 模块 | 协作线程、评论、版本、文档协同调用 |
| `customerOpportunity.js` | API 模块 | 客户机会中心的真实接口、响应规范和转项目闭环 |
| `dashboard.js` | API 模块 | Dashboard 总览、统计、任务和日历调用 |
| `export.js` | API 模块 | 导出任务、导出状态和格式枚举 |
| `fees.js` | API 模块 | 费用申请、审批、退还等调用 |
| `knowledge.js` | API 模块 | 资质、案例、模板等知识资产调用 |
| `qualification.js` | API 模块 | 资质 CRUD 与借阅记录/借阅申请接线，供知识页和 store 复用 |
| `projectGroups.js` | API 模块 | 项目组正式领域模型的管理、删除与项目绑定配置 |
| `projects.js` | API 模块 | 项目列表、详情、任务、评分、结果录入调用 |
| `resources.js` | API 模块 | 平台账号、BAR、证书、资源能力调用 |
| `settings.js` | API 模块 | 系统设置页的数据权限与组织树读写 |
| `tenders.js` | API 模块 | 标讯列表、详情、入项和关联调用 |
| `workflowForm.js` | API 模块 | 流程表单模板读取和表单实例提交，供 OA 审批类业务入口复用 |

`auth.js` 返回的用户快照会保留会话级权限字段，例如 `allowedProjectIds` 和 `allowedDepts`，供 store 和路由恢复使用。

- 2026-04-19: 新增 `qualification.js`，把资质 CRUD 从 `knowledge.js` 拆出，并为借阅接口未接入场景提供统一的前端未接入态响应。
- 2026-04-19: `knowledge.js` 的案例列表改为携带查询参数请求真实接口，并在模块内统一做分页/筛选整形，供案例页和详情页复用。
- 2026-04-22: 新增 `bidAgent.js`，通过真实项目 API 接入标书写作 Agent 的 run/status/apply/review 生命周期。
- 2026-04-24: 新增 `bidMatchScoring.js`，接入自定义投标匹配评分模型和标讯评分结果真实 API。
- 2026-04-29: 新增 `workflowForm.js`，资质借阅申请改为通过流程表单中心提交并触发 OA。
