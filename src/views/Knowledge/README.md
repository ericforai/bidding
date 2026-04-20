# Knowledge 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责说明
知识模块负责案例、资质、模板等知识资产页面。
该目录主要提供可复用资产的查询、展示、使用和维护入口。
页面内容以真实知识数据和业务操作为主，不承载通用组件实现。

## 边界清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `Case.vue` | View | 案例列表页 |
| `CaseDetail.vue` | View | 案例详情页 |
| `Qualification.vue` | View | 资质文件页编排层，复用 `components/qualification/` 下的列表、借阅记录与对话框组件 |
| `Template.vue` | View | 模板库页编排层，复用 `components/template/` 下的筛选区、列表与对话框组件 |

## 最近更新

- 2026-04-19: 资质页拆分为页面编排层 + `components/qualification/` 子组件，并移除页面内硬编码借阅记录。
- 2026-04-19: 模板库页拆分为页面编排层 + `components/template/` 子组件，并补齐产品类型、行业、文档类型三维分类筛选与编辑表单。
