---
title: 组织架构对接 - 客户事件库 SDK 方案
space: engineering
category: integration
tags: [integration, organization, event-sdk, event-bus, data-scope]
sources:
  - .wiki/sources/customer/事件库SDK接入说明方案.doc
  - .wiki/extracts/customer__事件库SDK接入说明方案.doc.md
  - backend/src/main/java/com/xiyu/bid/entity/User.java
  - backend/src/main/java/com/xiyu/bid/dto/DataScopeConfigPayload.java
backlinks:
  - _index
created: 2026-04-28
updated: 2026-04-28
health_checked: 2026-04-29
---
# 组织架构对接 - 客户事件库 SDK 方案

## 结论

客户提供的《事件库SDK接入说明方案》不是传统组织架构 REST API 文档，而是事件总线接入规范。西域数智化投标管理平台应把组织架构对接设计为“事件驱动同步”：

- Java 接入优先嵌入客户 `ClientSDK`，通过 `@AcceptEvent` 消费组织、部门、人员、岗位/角色变更事件。
- 若生产环境无法直接引入 SDK，则采用客户文档中的“非 Java 工程接入 SDK 方案”，由事件中转服务通过 HTTPS POST 推送到平台。
- 平台内部仍保持真实后端 API 单一路径：前端只读平台后端的用户、部门、权限配置接口，不直接连接客户事件库或 mock 数据。

## 客户接入规范摘要

| 项 | 客户文档要求 |
|---|---|
| Java SDK 坐标 | `com.ehsy.eventlibrary:ClientSDK:${eventlibrary.version}`，当前版本 `release_0.0.1` |
| 注册配置 | `client.register.serviceName`、`serverRegisterUrl`、`enableRegister` |
| 续约配置 | `client.renewal.initialDelay`、`period`、`renewalDuration` |
| 同步发送事件 | `SystemInteractiveService.sendEvent(eventCode, eventSource, eventContent, eventTrackReq)` |
| 异步发送事件 | `SystemInteractiveService.asyncSendEvent(...)` |
| 消费事件 | 方法增加 `@AcceptEvent(eventTopic = "...", consumerGroup = "...")`，入参必须为 `String eventMessage` |
| 消费响应 | 返回类型继承 `EventResult`；成功 `code=200`，失败 `code=500` 并填写 `msg` |
| HTTP 中转接收 | 客户中转服务 POST 到业务系统提供的 URL |
| HTTP 请求头 | `EHSY-TraceID`、`EHSY-SRCAPP`、`Content-Type: application/json` |
| HTTP 接收参数 | `eventTopic:String`、`eventMessage:String` |
| HTTP 发送 URL | `/api/event/sendEvent`、`/api/event/asyncSendEvent` |

## 对平台现状的映射

| 客户组织概念 | 平台落点 | 当前状态 | 处理策略 |
|---|---|---|---|
| 部门编码 | `DataScopeConfigPayload.DepartmentNode.departmentCode` | 已有配置 DTO | 新增持久化部门表或复用现有设置存储前需确认数据源 |
| 部门名称 | `departmentName` / `User.departmentName` | 已有字段 | 组织事件落库后同步用户快照 |
| 上级部门 | `parentDepartmentCode` | 已有 DTO 字段 | 保留树结构，用于数据权限和部门选择 |
| 用户账号 | `User.username` | 已有唯一字段 | 建议映射客户侧工号/登录名，避免用姓名做主键 |
| 用户姓名 | `User.fullName` | 已有字段 | 跟随人员事件增量更新 |
| 用户手机号 | `User.phone` | 已有字段 | 作为辅助匹配，不作为唯一主键 |
| 用户邮箱 | `User.email` | 已有唯一字段 | 客户未提供时需生成不可登录占位邮箱或放宽约束 |
| 角色/岗位 | `User.role` / `RoleProfile` | 已有 RBAC | 通过映射表转换为 `admin/manager/staff` 或具体 `RoleProfile` |
| 启停用 | `User.enabled` | 已有字段 | 人员离职/停用事件置为 false，不物理删除 |

## 目标架构

```text
客户组织系统
  |
  | 组织/部门/人员变更事件
  v
客户事件总线 / 事件库 SDK
  |
  | Java SDK @AcceptEvent
  | 或 HTTPS POST 中转
  v
西域后端 integration.organization
  |
  | 解析、幂等、校验、差异计算
  v
用户/部门/角色映射持久化
  |
  | 统一读模型
  v
系统设置、用户管理、数据权限、项目访问守卫
```

## 推荐后端模块拆分

遵循 FP-Java Profile，将事件解析与业务规则放在纯核心，SDK/HTTP/JPA 放在副作用边界。

```text
backend/src/main/java/com/xiyu/bid/integration/organization/
├── domain/
│   ├── OrganizationEvent.java              # 不可变事件值对象
│   ├── OrganizationEventType.java          # DEPARTMENT_UPSERT / USER_UPSERT / DISABLE 等
│   ├── OrganizationSyncCommand.java        # 纯核心输入命令
│   ├── OrganizationSyncPlan.java           # 纯核心输出：待新增/更新/停用动作
│   ├── OrganizationEventParser.java        # JSON -> 领域事件，返回 Result
│   ├── OrganizationSyncPolicy.java         # 差异计算、角色映射、幂等规则
│   └── OrganizationValidationResult.java
├── application/
│   ├── OrganizationEventAppService.java    # 事务编排、查库、保存、审计
│   ├── OrganizationEventGateway.java       # SDK/HTTP 入口统一调用接口
│   └── OrganizationSyncAuditService.java   # 同步结果记录
├── controller/
│   └── OrganizationEventWebhookController.java # HTTP 中转接收入口
├── sdk/
│   └── OrganizationEventSdkConsumer.java   # @AcceptEvent SDK 消费适配
├── dto/
│   ├── OrganizationEventWebhookRequest.java
│   └── OrganizationEventWebhookResponse.java
└── infrastructure/persistence/
    ├── entity/DepartmentEntity.java
    ├── entity/OrganizationEventLogEntity.java
    ├── repository/DepartmentRepository.java
    └── repository/OrganizationEventLogRepository.java
```

## 纯核心规则

| 规则 | 说明 |
|---|---|
| 幂等键 | 优先使用客户事件唯一 ID；若无，使用 `eventTopic + traceId + payloadHash` |
| 部门先于人员 | 人员所属部门不存在时，先创建“待确认部门”或将事件进入重试队列 |
| 不物理删除用户 | 离职、禁用、删除类事件统一映射为 `enabled=false` |
| 角色默认降级 | 未识别岗位默认 `STAFF`，并记录告警，禁止自动升为管理员 |
| 邮箱缺失处理 | 在确认客户字段前，采用临时占位策略必须经过产品/安全确认 |
| 数据权限联动 | 部门变更后刷新用户数据范围快照，但项目授权不自动扩大 |
| 失败可重放 | 解析失败、外键缺失、约束冲突均记录原始事件和失败原因，支持后台重放 |

## 事件 Topic 设计建议

需与客户最终确认事件编码。建议平台先按内部语义预留映射表：

| 内部语义 | 建议客户 Topic | 处理动作 |
|---|---|---|
| 部门新增/更新 | `org.department.upsert` | upsert 部门树节点 |
| 部门禁用/删除 | `org.department.disable` | 标记部门不可选，历史用户保留 |
| 人员新增/更新 | `org.user.upsert` | upsert 用户基础信息与组织归属 |
| 人员离职/停用 | `org.user.disable` | `User.enabled=false` |
| 岗位/角色变更 | `org.user.role.changed` | 更新 `RoleProfile` 映射 |
| 全量同步完成 | `org.sync.snapshot.completed` | 对账、生成差异报告 |

## HTTP 中转接口草案

```http
POST /api/integrations/organization/events
EHSY-TraceID: <trace-id>
EHSY-SRCAPP: <source-app>
Content-Type: application/json
```

```json
{
  "eventTopic": "org.user.upsert",
  "eventMessage": "{\"userCode\":\"u001\",\"name\":\"张三\",\"departmentCode\":\"sales-east\"}"
}
```

平台响应保持客户文档约定：

```json
{
  "code": "200",
  "msg": "success",
  "timestamp": 1777359048000,
  "data": {
    "eventId": "org.user.upsert:trace:hash",
    "accepted": true
  }
}
```

## 配置项

```yaml
xiyu:
  integrations:
    organization:
      enabled: false
      mode: sdk # sdk | webhook
      consumer-group: xiyu-bid
      sdk:
        service-name: XiyuBidService
        server-register-url: https://event-busserver.ehsy.com
        enable-register: true
      security:
        allowed-source-apps:
          - customer-org
        allowed-ips: []
      mapping:
        default-role: staff
        admin-role-codes: []
        manager-role-codes: []
```

## 实施阶段

### 阶段 0：客户澄清

1. 获取组织架构事件 Topic 清单、样例 `eventMessage`、字段字典、事件唯一 ID 规则。
2. 确认全量初始化方式：事件快照、批量文件、数据库视图或一次性 API。
3. 确认人员唯一键：工号、AD 账号、手机号、邮箱哪个为主键。
4. 确认部门删除语义：物理删除、停用、合并、迁移。
5. 确认网络与安全：事件总线地址、生产域名、IP 白名单、HTTPS 证书、是否有签名。

### 阶段 1：最小闭环

1. 新增组织事件 domain 纯核心与单元测试。
2. 新增 HTTP webhook 接收入口，先不依赖 SDK，便于本地和联调环境验证。
3. 新增事件日志表，记录 trace、source、topic、payload hash、处理状态。
4. 新增部门持久化模型或确认复用现有配置存储。
5. 将用户 upsert 接到 `User`、`RoleProfile`、数据权限配置读模型。

### 阶段 2：SDK 正式接入

1. 将客户 `ClientSDK` 通过私服或本地仓库引入后端。
2. 增加 `OrganizationEventSdkConsumer`，用 `@AcceptEvent` 消费客户确认的 Topic。
3. SDK 入口只做边界转换，统一委托 `OrganizationEventAppService`。
4. 增加 SDK 开关，生产可选择 `sdk`，测试/灾备可切到 `webhook`。

### 阶段 3：对账与运维

1. 建立全量组织快照对账任务，输出新增、缺失、字段冲突、部门孤儿节点。
2. 在系统设置中增加组织同步状态页：最近同步时间、失败事件、重放入口。
3. 增加告警：连续失败、未知角色、未知部门、管理员映射变更。
4. 建立上线回滚：关闭消费开关、停止写用户、保留事件日志待重放。

## 验证计划

| 范围 | 命令 / 动作 |
|---|---|
| 纯核心 | `mvn test -Dtest=OrganizationEventParserTest,OrganizationSyncPolicyTest` |
| 架构门禁 | `mvn test -Dtest=FPJavaArchitectureTest,MaintainabilityArchitectureTest` |
| 权限边界 | 若新增/修改带项目关联的用户可见范围，运行 `mvn test -Dtest=ProjectAccessGuardCoverageTest` |
| API 契约 | `mvn test -Dtest=OrganizationEventWebhookControllerTest` |
| 前端配置页 | `npx vitest run <相关测试文件>`、`npm run check:front-data-boundaries`、`npm run build` |
| 联调 | 使用客户样例事件分别验证部门新增、人员新增、人员转部门、人员停用、未知角色 |

## 风险与决策

| 风险 | 等级 | 建议 |
|---|---|---|
| 文档未提供组织事件 payload 字段 | 高 | 不进入开发排期前必须补齐样例与字段字典 |
| SDK 只能从客户私服获取 | 中 | 提前开通 Maven 私服访问或提供离线 jar 入库流程 |
| 事件无签名，仅 IP 白名单 | 中 | 至少增加来源应用白名单、trace 记录、可选共享密钥 |
| 用户邮箱唯一约束与客户字段不匹配 | 高 | 设计 external_user_id，并评估用户表唯一约束迁移 |
| 部门变更影响项目数据权限 | 高 | 先更新组织归属，不自动扩大项目可见范围 |
| 全量与增量并存导致乱序 | 中 | 使用事件时间、版本号或最后更新时间做冲突解决 |

## 待客户确认问题

1. 组织架构相关事件 Topic 和每个 Topic 的 `eventMessage` JSON 样例是什么？
2. `eventMessage` 是否包含事件 ID、事件时间、操作类型、版本号？
3. 人员唯一标识是什么？是否可能变更？
4. 邮箱、手机号是否必填且唯一？
5. 部门是否存在排序、区域、成本中心、业务线等扩展字段？
6. 角色/岗位如何映射到平台 `admin/manager/staff` 与自定义 `RoleProfile`？
7. 是否要求平台向客户系统回传同步结果事件？
8. 生产事件总线地址、测试事件总线地址、IP 白名单和证书要求是什么？
