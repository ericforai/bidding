---
title: 角色与权限
space: engineering
category: reference
tags: [角色, 权限, 用户, 认证, RBAC]
sources:
  - .wiki/sources/implementation/西域数智化投标管理平台实施计划书SOW2026V1.4(格式校准).docx
  - src/router/index.js
  - AGENTS.md
  - backend/src/main/java/com/xiyu/bid/auth/README.md
backlinks:
  - _index
  - business-process
  - contract-constraints
  - overview
created: 2026-04-15
updated: 2026-04-24
health_checked: 2026-04-24
---
# 角色与权限

## 1. 角色体系

平台采用 RBAC（基于角色的访问控制）模型，当前定义三个核心角色：

| 角色 ID | 角色名 | 中文名 | 权限范围 |
|---------|--------|--------|----------|
| admin | 管理员 | 管理员 | 全部功能权限，包括系统设置、用户管理、数据分析、所有业务模块 |
| manager | 经理 | 经理 | 所有业务操作权限 + 数据分析看板，不含系统设置 |
| sales / staff | 销售 / 普通员工 | 销售人员 / 技术人员 | 标讯跟进、项目执行、任务处理、费用申请、知识库使用等日常业务操作 |

## 2. 联调与演示账号

联调、演示和验收统一使用 API 模式与后端真实鉴权体系。当前约定账号如下：

| 用户名 | 角色 | 用途 |
|--------|------|------|
| 小王 | 销售 / 普通员工（sales/staff） | 标讯跟进、项目执行、任务处理、费用申请 |
| 张经理 | 经理（manager） | 项目审批、任务分配、进度监控、数据分析 |
| 李总 | 管理员（admin） | 系统设置、全局数据分析、用户与权限管理 |

## 3. 路由级权限

路由通过 `meta.roles` 字段控制角色访问。未配置 `meta.roles` 的路由对所有已登录用户开放。

### 需要特定角色的路由

| 路由路径 | 路由名称 | 页面标题 | 允许角色 |
|----------|----------|----------|----------|
| `/analytics/dashboard` | AnalyticsDashboard | 数据分析 | admin, manager |
| `/settings` | Settings | 系统设置 | admin |

### 对所有已登录用户开放的路由

| 路由路径 | 路由名称 | 页面标题 |
|----------|----------|----------|
| `/dashboard` | Dashboard | 工作台 |
| `/bidding` | Bidding | 标讯中心 |
| `/bidding/:id` | BiddingDetail | 标讯详情 |
| `/bidding/ai-analysis/:id` | BiddingAIAnalysis | AI 分析 |
| `/bidding/customer-opportunities` | CustomerOpportunityCenter | 客户商机中心 |
| `/ai-center` | AICenter | AI 智能中心 |
| `/project` | ProjectList | 投标项目 |
| `/project/create` | ProjectCreate | 创建项目 |
| `/project/:id` | ProjectDetail | 项目详情 |
| `/knowledge/qualification` | Qualification | 资质库 |
| `/knowledge/case` | Case | 案例库 |
| `/knowledge/case/detail` | CaseDetail | 案例详情 |
| `/knowledge/template` | Template | 模板库 |
| `/resource/expense` | Expense | 费用管理 |
| `/resource/account` | Account | 账户管理 |
| `/resource/bid-result` | BidResult | 投标结果闭环 |
| `/resource/bar` | BAR | 可投标能力检查 |
| `/resource/bar/sites` | BAR_SiteList | 站点台账 |
| `/resource/bar/site/:id` | BAR_SiteDetail | 站点详情 |
| `/resource/bar/sop/:siteId` | BAR_SOPDetail | 找回 SOP |
| `/document/editor/:id` | DocumentEditor | 标书编辑器 |

### API 模式路由治理

API 模式是唯一交付路径。未纳入 SOW V1.4、蓝图确认件或正式变更单的演示性入口，应在客户环境隐藏或重定向。

| 类型 | 处理口径 |
|---|---|
| SOW/蓝图确认范围内 | 按角色权限开放 |
| 演示性、未闭环、未确认范围 | 隐藏、下线或替换处理 |
| 需要新增的权限或流程 | 进入变更评估，书面确认后实施 |

### 路由守卫逻辑

1. 未登录用户访问需要认证的页面时，自动重定向到 `/login`。
2. 已登录用户访问 `/login` 时，自动重定向到 `/dashboard`。
3. 已登录用户访问权限不足的页面时，自动重定向到 `/dashboard`。
4. 首次加载时会尝试恢复 localStorage 中的会话状态。

## 4. 认证机制

平台采用 JWT + Spring Security 的认证授权架构。

### 认证流程

```
用户登录 -> AuthController 验证凭据 -> JwtUtil 生成 JWT 令牌 -> 返回令牌给前端
    |
前端存储令牌 -> 后续请求携带 Authorization: Bearer <token>
    |
JwtAuthenticationFilter 拦截请求 -> JwtUtil 校验令牌 -> UserDetailsServiceImpl 加载用户
    |
Spring Security 上下文注入用户信息 -> 业务接口正常响应
```

### 后端 Auth 模块组成

| 组件 | 职责 |
|------|------|
| `JwtUtil.java` | JWT 令牌的生成、解析和验证 |
| `JwtAuthenticationFilter.java` | HTTP 请求拦截器，从请求头提取并校验 JWT |
| `UserDetailsServiceImpl.java` | Spring Security 用户详情服务，从数据库加载用户信息 |
| `AuthController.java` | 登录/注册 API 端点 |
| `User.java` | 用户实体定义 |

### 前端会话管理

- 登录成功后，令牌和用户信息存储在 Pinia store 并持久化到 localStorage。
- 页面刷新时通过 `restoreSession()` 从 localStorage 恢复会话。
- 收到 401 响应时自动清除 store 状态并重定向到登录页。

详细架构说明请参阅 [[architecture]]。

## 5. 数据权限

平台支持数据范围配置，根据用户角色和组织归属控制可见数据的范围：

| 角色 | 数据可见范围 |
|------|-------------|
| admin | 全部数据，跨部门、跨区域 |
| manager | 本部门及下属团队数据 |
| sales / staff | 仅限个人负责的数据 |

数据权限通过后端的 DataScopeConfig 机制实现，支持按组织架构层级配置数据隔离规则。

## 6. 与业务流程的关联

不同角色在投标全流程（参见 [[business-process]]）各阶段承担不同职责：

| 阶段 | admin（管理员） | manager（经理） | sales/staff（销售/技术） |
|------|-----------------|-----------------|-------------------------|
| 标讯获取 | 全局标讯监控 | 分配标讯、指派跟进 | 领取标讯、跟进反馈 |
| 项目立项 | - | 立项审批、资源协调 | 创建项目、填写信息、CRM 同步 |
| 任务分解 | - | 审核任务分配 | 执行任务、上传交付物 |
| 标书编制 | - | 审核标书、用印审批 | 技术方案编制、协同编辑 |
| 投标提交 | - | 用印审批、封装确认 | 提交投标文件、状态跟踪 |
| 结果闭环 | 全局数据分析、经营决策 | 结果分析、团队复盘 | 中标登记、竞对信息录入 |
| 数据分析 | 管理驾驶舱、全维度分析 | 部门级分析看板 | - |
| 系统设置 | 用户管理、参数配置、日志审计 | - | - |
