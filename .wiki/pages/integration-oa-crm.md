---
title: OA 与 CRM 对接规范
space: engineering
category: integration
tags: [integration, oa, crm, workflow, api]
sources:
  - README.md
backlinks:
  - _index
  - integration-organization-event-sdk
created: 2026-05-07
updated: 2026-05-07
health_checked: 2026-05-07
---
# OA 与 CRM 对接规范

## 1. OA 流程接入 (Workflow)

投标系统通过西域后端中转，实现与客户 OA 系统的流程发起与状态同步。

### 1.1 核心接口
- **创建流程**：`POST /oaWorkflow/createWorkflow`
- **中转逻辑**：投标系统提交业务字段 -> 西域 Backend 映射 -> 调用 OA 接口。

### 1.2 支持流程清单 (workflowId)
1. 西域集团-公章盖章申请单
2. 报价章盖章申请单
3. 印章及证件借用申请单
4. 西域-投标资料包申请流程
5. 一般付款流程
6. 费用报销流程
7. 借款与保证金申请流程
8. 借款与保证金核销流程

### 1.3 请求报文规范
- **userNo**: 申请人 OA 账号。
- **mainData**: 主表业务数据 (Key 为源字段名)。
- **attachmentList**: 附件列表 (推荐传 URL)。

### 1.4 回调机制 (Callback)
- **状态枚举**: `REJECT` (驳回), `ARCHIVE` (归档), `SUPPLIER_SIGNING` (供应商签署)。
- **数据回流**: 归档后需将 OA 生成的正式 PDF 文件回传至投标系统。

---

## 2. CRM 接口对接 (Customer Relationship)

用于在投标系统内直接检索客户信息、客户经理及跟进状态。

### 2.1 接入要求
- **协议**: HTTPS
- **鉴权**: Token 鉴权 (Authorization Header)

### 2.2 接口列表
1. **登录鉴权接口**：获取访问 Token。
2. **登出接口**：作废 Token。
3. **客户模糊查询**：根据名称查询有效客户列表。
4. **负责人查询**：根据公司 ID 列表查询对应的客户经理/负责人。

---

## 3. 验收标准

| 阶段 | 关键项 | 成功标志 |
|---|---|---|
| OA 创建 | 流程发起 | 返回 `requestId` 且 OA 表单可正确打开 |
| OA 字段 | 映射一致性 | OA 表单内容与投标系统提交值完全一致 |
| OA 回调 | 状态同步 | OA 审批通过后，投标系统标书状态自动更新 |
| CRM 检索 | 实时查询 | 输入客户名能快速联想出 CRM 存量数据 |

---

## 相关文档
- [[integration-organization-event-sdk]] 组织架构对接方案
- [[architecture]] §5 API 集成层
