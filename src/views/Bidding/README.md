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
| `CustomerOpportunityCenter.vue` | View | 客户机会中心页 |

## Customer Opportunity 拆分

- `CustomerOpportunityCenter.vue` 仅保留页面壳、路由跳转和页面级动作。
- `customerOpportunityView.js` 承载纯 view util，包括筛选、选中客户拼装、drawer 聚合和 create project query 组装。
- `customer-opportunity/` 目录承载客户机会中心的展示组件：顶部概览、客户池、详情区、历史 drawer。
