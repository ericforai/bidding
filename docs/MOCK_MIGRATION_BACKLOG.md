# Mock 直连页面迁移 Backlog

本清单只记录“页面层仍直接依赖 `@/api/mock`”的剩余迁移点。
`src/api/mock.js` 继续作为长期 MVP 演示数据源保留，不在本清单中视为待删除资产。

## 已完成首批主链路

- 登录页：已切到 `authApi + user store`
- 项目列表页：已切到 `projectsApi + project store`
- 标讯列表主数据：已切到 `tendersApi + bidding store`

## 第二阶段待迁移页面

| 页面 | 当前直连 Mock 内容 | 推荐迁移目标 |
| --- | --- | --- |
| `src/views/Analytics/Dashboard.vue` | 看板统计、趋势图表数据 | `dashboardApi`，先按后端 `/api/analytics/*` 实际路径收敛 |
| `src/views/Bidding/AIAnalysis.vue` | AI 分析结果 | `aiApi` 或 `tendersApi` 的真实分析契约，先统一响应结构 |
| `src/views/Project/Detail.vue` | 项目详情、任务、文档 | `projectsApi`，其中任务/文档子能力需先明确后端契约 |
| `src/views/Resource/Expense.vue` | 费用台账与统计 | 先在 `feesApi` 和 `resources/expenses` 两套模型中确定唯一事实源 |
| `src/views/Resource/Account.vue` | 账户列表与借用状态 | 先区分 `platform accounts` 与 `resource accounts` 后再迁移 |
| `src/views/Resource/BAR/components/BorrowDialog.vue` | 站点借用/找回演示数据 | 先收敛 BAR 领域模型，再决定是否接 `bar-assets` 或新增后端接口 |
| `src/views/System/Settings.vue` | API 文档与系统展示数据 | 改为基于真实契约生成，避免继续传播错误路径 |

## Store 层仍保留 Mock 的位置

以下 store 当前仍保留 mock 作为 fallback 或演示数据来源，属于允许状态，不视为本阶段问题：

- `src/stores/user.js`
- `src/stores/project.js`
- `src/stores/bidding.js`
- `src/stores/bar.js`

## 迁移规则

- 已迁移页面禁止再次直接 `import { mockData } from '@/api/mock'`
- 未迁移页面允许继续读 mock，但必须先修正对应 API 模块契约再迁移
- 对后端不存在的能力，API 层必须显式失败，不允许伪造真实接口成功
