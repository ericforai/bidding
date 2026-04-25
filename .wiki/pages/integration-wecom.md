---
title: 系统集成中心 - 企业微信
space: engineering
category: integration
tags: [integration, wecom, settings, sso, message-push]
sources:
  - backend/src/main/java/com/xiyu/bid/integration/
  - src/views/System/settings/SystemIntegrationPanel.vue
  - src/views/System/settings/integration/
backlinks:
  - api-openapi
  - architecture
  - requirements
created: 2026-04-25
updated: 2026-04-25
health_checked: 2026-04-25
---

# 系统集成中心 - 企业微信

> 满足客户「集成性」需求条款的统一配置入口，落地企业微信首支持，CRM/OA/组织架构占位待实施。

## 客户需求溯源

合同条款「集成性」要求：

- ✅ **与组织架构系统集成** — 占位（计划）
- ✅ **与 OA / 审批流集成** — 占位（计划）
- ✅ **与 CRM 系统集成** — 占位（计划）
- ✅ **开放 API 接口** — 已通过 [[api-openapi]] 落地
- ✅ **与企业微信集成** — **本次落地**：配置入口 + SSO/消息推送启用开关 + 连接测试

## 功能落地状态

| 集成对象 | 状态 | 说明 |
|---|---|---|
| **企业微信** | ✅ 配置入口 + 连通性测试（Mock） | SSO 与消息推送的实际收发逻辑下一期 |
| **CRM 系统** | ⚪ 占位「即将支持」 | 待客户提供 CRM 接口规范 |
| **OA / 审批流** | ⚪ 占位「即将支持」 | 待选定 OA 厂商 |
| **组织架构系统** | ⚪ 占位「即将支持」 | 待客户内部架构系统接口 |

## 入口路径

`系统设置 → 系统集成` Tab（`/settings`，仅 ADMIN 角色可见）

## 数据模型

`wecom_integration` 表（V87 迁移）单行配置（`id=1`）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `corp_id` | VARCHAR(64) | 企业 CorpID |
| `agent_id` | VARCHAR(32) | 应用 AgentID（数字） |
| `encrypted_secret` | TEXT | 应用 Secret（AES-256-GCM 加密） |
| `sso_enabled` | BOOLEAN | 是否启用单点登录 |
| `message_enabled` | BOOLEAN | 是否启用应用消息推送 |
| `updated_at` | TIMESTAMP | 自动更新 |
| `updated_by` | VARCHAR(64) | 操作人 |

加密委托给现有 `com.xiyu.bid.platform.util.PasswordEncryptionUtil`（AES-256-GCM）。

## REST API

| Method | Path | 行为 |
|---|---|---|
| `GET` | `/api/admin/integrations/wecom` | 读取配置；**Secret 永不回显**，仅返回 `secretConfigured:boolean` |
| `PUT` | `/api/admin/integrations/wecom` | 保存/更新；`corpSecret` 为空时保留原值 |
| `POST` | `/api/admin/integrations/wecom/test` | 用当前配置调连通性 Probe，返回 `{success, message, probedAt}` |

所有路径已受 `SecurityConfig` ADMIN 角色保护，自动出现在 [[api-openapi]] Swagger UI 里。

## 后端模块结构（FP-Java + Split-First）

```
backend/src/main/java/com/xiyu/bid/integration/
├── domain/                    # 纯核心，零 Spring 依赖
│   ├── WeComCredential.java          # 不可变值对象（toString 屏蔽 secret）
│   ├── WeComCredentialValidation.java # 纯函数验证（防御纵深）
│   ├── WeComConnectivityResult.java
│   └── ValidationResult.java
├── application/               # 编排层，无 DTO 转换
│   ├── WeComIntegrationAppService.java   # @Service @Transactional
│   ├── WeComConnectivityProbe.java       # 接口
│   ├── WeComMockConnectivityProbe.java   # @Component 默认 Mock
│   └── WeComCredentialCipher.java        # 加解密门面
├── controller/                # 仅 Request/Response 转换 + 异常处理
│   └── WeComIntegrationController.java
├── dto/                       # 输入输出契约
│   ├── WeComIntegrationRequest.java   # @NotBlank/@Pattern 字段校验
│   ├── WeComIntegrationResponse.java  # 不含 corpSecret 字段
│   └── WeComConnectivityResponse.java
└── infrastructure/persistence/
    ├── entity/WeComIntegrationEntity.java
    └── repository/WeComIntegrationJpaRepository.java
```

每个文件 ≤ 85 行；最大 `WeComIntegrationAppService` 85 行。所有类单一职责，无任何类同时承担「规则计算 + 数据访问 + DTO 转换 + 状态写入」3 类以上。

## 前端结构

```
src/api/modules/systemIntegration.js         # axios 封装
src/views/System/Settings.vue                 # 新增 Tab 接入
src/views/System/settings/SystemIntegrationPanel.vue  # 容器（企微 + 占位卡）
src/views/System/settings/integration/
├── WeComIntegrationCard.vue                  # 配置表单 + 测试按钮
└── IntegrationComingSoonCard.vue             # 占位卡（CRM/OA/组织架构）
src/views/System/settings/useWeComSettings.js # composable（唯一调 API 模块）
```

数据边界：组件不直接调 axios；composable 是 API 唯一入口，符合项目 `check:front-data-boundaries` 治理规则。

## 安全设计

1. **Secret 永不回显**：`WeComIntegrationResponse` DTO 已移除 `corpSecret` 字段，`secretConfigured:boolean` 替代
2. **toString 屏蔽**：`WeComCredential.toString()` 重写，输出 `corpSecret=***`
3. **存储加密**：AES-256-GCM，依赖 `PLATFORM_ENCRYPTION_KEY` 环境变量（参考 `PasswordEncryptionUtil`）
4. **单元测试断言**：`WeComCredentialTest.toString_maskSecret()` 阻止 toString 泄露回归
5. **Bean Validation + Domain Validation 双层防御**：HTTP 边界拦字段格式，绕过 Controller 的调用方仍受 domain 校验保护

## 测试覆盖

| 模块 | 测试数 | 文件 |
|---|---|---|
| `WeComCredentialTest` | 6 | 不可变性 + toString 屏蔽 |
| `WeComCredentialValidationTest` | 7 | 各字段错误分支 |
| `WeComIntegrationAppServiceTest` | 9 | 编排（加密、Secret 不泄露、连通性） |
| `WeComMockConnectivityProbeTest` | 2 | Mock probe 行为 |
| `WeComIntegrationControllerTest` | 7 | REST 状态码 + 异常处理 |
| 前端 `systemIntegration.spec.js` | 6 | API 模块行为 |
| 前端 `useWeComSettings.spec.js` | 13 | composable load/save/test/error |

## 后续工作（已知缺口）

| 项 | 说明 |
|---|---|
| SSO OAuth2 实际回调 | 落地完整登录流程 + state token 校验 |
| 应用消息实际推送 | 接 `https://qyapi.weixin.qq.com/cgi-bin/message/send` |
| 通讯录同步 | 拉取部门/成员到 user/department 表 |
| 真实连通性 Probe | 替换 Mock 为真实 access_token 调用 |
| CRM/OA/组织架构对接 | 等客户提供接口规范后实施 |

## 变更历史

| 日期 | 提交 | 变更 |
|---|---|---|
| 2026-04-25 | （worktree 待合并） | feat: 系统集成 Tab + 企业微信配置能力 |
