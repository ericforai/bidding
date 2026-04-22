# Bidding 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责说明
标讯模块负责标讯列表、详情、结果分析和客户机会中心等页面级能力。
该目录承载投标前端主流程入口，不放通用组件和跨模块基础逻辑。
页面内的状态管理、查询与交互均围绕标讯业务展开。

## 边界清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `List.vue` | View | 标讯列表页 |
| `Detail.vue` | View | 标讯详情页 |
| `AIAnalysis.vue` | View | 标讯 AI 分析页 |
| `CustomerOpportunityCenter.vue` | View | 客户机会中心容器页 |
| `list/*` | View / Composable / UI | 标讯列表页局部组件、组合逻辑、样式与安全纯函数 |
| `detail/*` | View / Composable / UI | 标讯详情页局部组件、组合逻辑与样式 |
| `ai-analysis/*` | View / Composable / UI | AI 分析页局部组件、组合逻辑与样式 |
| `customer-opportunity/*` | View / Composable / UI | 客户池、看板、详情、历史抽屉与组合逻辑 |

## 更新记录

- 2026-04-22: `List.vue` 拆为页面壳和 `list/*` 局部模块；批量领取改为服务端认证用户，删除改真实 API，标讯源密钥不再写入本地存储
- 2026-04-22: `AIAnalysis.vue` 与 `Detail.vue` 拆为页面壳，新增 `ai-analysis/*` 与 `detail/*` 局部模块并增加行数预算门禁
- 2026-04-21: `List.vue` 搜索改为刷新后端标讯检索结果，人工录入改为调用真实标讯创建接口入库
