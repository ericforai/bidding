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
updated: 2026-04-30
health_checked: 2026-04-30
---
# 组织架构对接 - 客户事件库 SDK 方案

## 结论

客户最新 PDF 口径明确：组织架构同步不是“事件 payload 直读主数据”，而是“事件库通知 + 接口回查主数据”。西域数智化投标管理平台应把组织架构对接设计为“事件驱动触发、主数据接口回查”的同步链路：

- 事件库只负责通知变化，事件 `data` 仅携带 `deptId` 或 `userId`，不得作为部门/用户主数据来源。
- 平台收到 `BaseOssDept` / `BaseOssUser` 事件后，必须通过客户组织架构主数据接口回查部门或人员详情，再更新平台用户、部门、角色映射和数据权限读模型。
- 客户 `ClientSDK` jar 当前未提供 Maven 私服或离线包，第一版先实现端口适配、HTTP 中转接收、HTTP 回查测试入口和灾备入口；后续拿到 jar 后只补 SDK adapter，不改应用服务和纯核心。
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

## 最新事件契约

事件库 Topic 以客户 PDF 为准，第一版只支持组织架构基础数据变更通知：

| Topic | 语义 | 事件 data 字段 | 后续动作 |
|---|---|---|---|
| `BaseOssDept` | 部门主数据发生变化 | `data.deptId` | 使用 `deptId` 回查部门主数据接口 |
| `BaseOssUser` | 用户主数据发生变化 | `data.userId` | 使用 `userId` 回查用户主数据接口 |

事件消息需要保留以下追踪与分发字段：

| 字段 | 说明 |
|---|---|
| `traceId` | 调用链追踪 ID，进入事件日志和应用日志 |
| `spanId` | 当前调用 span |
| `parentId` | 父级调用 span |
| `eventSource` | 事件来源系统 |
| `eventTopic` | 事件 Topic，当前支持 `BaseOssDept`、`BaseOssUser` |
| `time` | 事件发生或投递时间 |
| `key` | 事件 key，用于辅助幂等和排查 |
| `data.deptId` | 部门事件的部门 ID，仅用于回查 |
| `data.userId` | 用户事件的用户 ID，仅用于回查 |

示例：

```json
{
  "traceId": "trace-20260430-001",
  "spanId": "span-001",
  "parentId": "parent-000",
  "eventSource": "oss",
  "eventTopic": "BaseOssUser",
  "time": "2026-04-30T10:15:30+08:00",
  "key": "user-10001",
  "data": {
    "userId": "10001"
  }
}
```

禁止把上述事件 `data` 当作主数据 JSON 展开解析。除 `deptId` / `userId` 外的用户姓名、手机号、邮箱、部门名称、上级部门、启停用、岗位角色等字段均以回查接口返回为准。

## 对平台现状的映射

| 客户组织概念 | 平台落点 | 当前状态 | 处理策略 |
|---|---|---|---|
| 部门 ID | `DataScopeConfigPayload.DepartmentNode.departmentCode` 或新增外部部门 ID | 已有配置 DTO | 从 `BaseOssDept.data.deptId` 回查部门主数据后写入 |
| 部门名称 | `departmentName` / `User.departmentName` | 已有字段 | 以部门主数据接口返回为准，不从事件 payload 读取 |
| 上级部门 | `parentDepartmentCode` | 已有 DTO 字段 | 以部门主数据接口返回为准，保留树结构 |
| 用户 ID | `User.username` 或新增 external user id | 已有唯一字段 | 从 `BaseOssUser.data.userId` 回查用户主数据后映射 |
| 用户姓名 | `User.fullName` | 已有字段 | 以用户主数据接口返回为准 |
| 用户手机号 | `User.phone` | 已有字段 | 作为辅助匹配，不作为唯一主键 |
| 用户邮箱 | `User.email` | 已有唯一字段 | 客户接口未提供时需生成不可登录占位邮箱或放宽约束 |
| 角色/岗位 | `User.role` / `RoleProfile` | 已有 RBAC | 以用户主数据或岗位接口返回为准，通过映射表转换 |
| 启停用 | `User.enabled` | 已有字段 | 以用户主数据状态为准，不物理删除 |

## 目标架构

```text
客户组织系统
  |
  | BaseOssDept / BaseOssUser 变更通知
  v
客户事件总线 / 事件库 SDK
  |
  | Java SDK @AcceptEvent
  | 或 HTTPS POST 中转
  v
西域后端 integration.organization
  |
  | 解析通知、幂等、校验、按 ID 回查主数据、差异计算
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
│   ├── OrganizationEventType.java          # BASE_OSS_DEPT / BASE_OSS_USER
│   ├── OrganizationSyncCommand.java        # 纯核心输入命令
│   ├── OrganizationSyncPlan.java           # 纯核心输出：待新增/更新/停用动作
│   ├── OrganizationEventParser.java        # JSON -> ID 通知事件，返回 Result
│   ├── OrganizationSyncPolicy.java         # 差异计算、角色映射、幂等规则
│   └── OrganizationValidationResult.java
├── application/
│   ├── OrganizationEventAppService.java    # 事务编排、回查主数据、保存、审计
│   ├── OrganizationEventGateway.java       # SDK/HTTP 入口统一调用接口
│   ├── OrganizationMasterDataGateway.java  # 客户组织主数据回查端口
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
| 事件只作通知 | `data.deptId` / `data.userId` 只能触发回查，不能直接写用户或部门主数据 |
| 幂等键 | 优先使用客户事件唯一 ID 或 `key`；若无，使用 `eventTopic + traceId + payloadHash` |
| 回查失败可重试 | 主数据接口超时、404、字段缺失、鉴权失败均记录事件日志并进入重试/人工处理 |
| 部门先于人员 | 用户回查结果中的所属部门不存在时，先回查部门或将事件进入重试队列 |
| 不物理删除用户 | 离职、禁用、删除类事件统一映射为 `enabled=false` |
| 角色默认降级 | 未识别岗位默认 `STAFF`，并记录告警，禁止自动升为管理员 |
| 邮箱缺失处理 | 在确认客户字段前，采用临时占位策略必须经过产品/安全确认 |
| 数据权限联动 | 部门变更后刷新用户数据范围快照，但项目授权不自动扩大 |
| 失败可重放 | 解析失败、外键缺失、约束冲突均记录原始事件和失败原因，支持后台重放 |

## 事件 Topic 支持范围

旧文档中 `org.user.upsert`、`org.department.upsert` 等内部语义 Topic 已废弃，不再表示客户事件 payload 可以直接 upsert 平台用户或部门。当前按客户 PDF 只接收以下 Topic：

| 客户 Topic | 内部语义 | 处理动作 |
|---|---|---|
| `BaseOssDept` | 部门变化通知 | 读取 `data.deptId`，回查部门主数据，再生成部门新增/更新/停用计划 |
| `BaseOssUser` | 用户变化通知 | 读取 `data.userId`，回查用户主数据，再生成用户新增/更新/停用与角色映射计划 |

## HTTP 中转接口草案

```http
POST /api/integrations/organization/events
EHSY-TraceID: <trace-id>
EHSY-SRCAPP: <source-app>
Content-Type: application/json
```

```json
{
  "eventTopic": "BaseOssUser",
  "eventMessage": "{\"traceId\":\"trace-20260430-001\",\"spanId\":\"span-001\",\"parentId\":\"parent-000\",\"eventSource\":\"oss\",\"eventTopic\":\"BaseOssUser\",\"time\":\"2026-04-30T10:15:30+08:00\",\"key\":\"user-10001\",\"data\":{\"userId\":\"10001\"}}"
}
```

平台响应保持客户文档约定：

```json
{
  "code": "200",
  "msg": "success",
  "timestamp": 1777359048000,
  "data": {
    "eventId": "BaseOssUser:trace:hash",
    "masterDataLookup": "scheduled",
    "accepted": true
  }
}
```

## 配置项

运行时优先读取系统设置页的 `integrationConfig`，`application.yml` 仅作为兜底默认值：

- `orgEnabled`：组织架构事件接入开关，关闭后 webhook/SDK 消费会拒绝并记录 REJECTED。
- `orgSystem`：组织系统/事件来源标识，会并入允许的 `eventSource` 列表。
- `orgAppKey`：客户侧应用标识，会并入允许的 `eventSource` 列表；真实主数据 adapter 落地后也作为鉴权参数来源。
- `orgAppSecret`：HTTP webhook HMAC 签名密钥；为空时回退到 `xiyu.integrations.organization.webhook-secret`。
- `ipWhitelist`：HTTP webhook 灾备入口来源 IP 白名单；为空表示不限制来源 IP。

仍保留的 yml 兜底配置：

```yaml
xiyu:
  integrations:
    organization:
      enabled: false
      webhook-secret: ""
      ip-whitelist: ""
      allowed-source-apps:
        - customer-org
      admin-role-codes: []
      manager-role-codes: []
```

## 实施阶段

### 阶段 0：客户澄清

1. 获取 `ClientSDK` jar、Maven 私服地址或离线入库流程。
2. 获取 YAPI 中 `BaseOssDept` / `BaseOssUser` 事件字段、部门主数据回查接口、用户主数据回查接口字段字典和样例。
3. 确认生产地址：事件库地址、主数据接口地址、测试地址、灾备地址。
4. 确认网络与安全：IP 白名单、HTTPS 证书、鉴权方式、签名方式、token 获取与过期策略。
5. 确认全量初始化方式：主数据分页接口、事件快照、批量文件、数据库视图或一次性 API。
6. 确认人员唯一键：工号、AD 账号、手机号、邮箱、`userId` 哪个为主键。
7. 确认部门删除语义：物理删除、停用、合并、迁移。

### 阶段 1：最小闭环

1. 新增组织事件 domain 纯核心与单元测试，只解析 `BaseOssDept` / `BaseOssUser` 通知和 ID。
2. 新增 HTTP webhook 接收入口，先不依赖 SDK，便于本地、联调、灾备和自动化测试验证。
3. 新增客户主数据 HTTP 回查端口与测试入口，用 `deptId` / `userId` 拉取详情后再生成同步计划。
4. 新增事件日志表，记录 trace、source、topic、key、payload hash、回查状态、处理状态。
5. 新增部门持久化模型或确认复用现有配置存储。
6. 将回查后的用户主数据接到 `User`、`RoleProfile`、数据权限配置读模型。

### 阶段 2：SDK 正式接入

1. 将客户 `ClientSDK` 通过私服或本地仓库引入后端。
2. 增加 `OrganizationEventSdkConsumer`，用 `@AcceptEvent` 消费客户确认的 Topic。
3. SDK 入口只做边界转换，统一委托 `OrganizationEventAppService`。
4. 增加 SDK 开关，生产可选择 `sdk`，测试/灾备可切到 `webhook`。
5. SDK adapter 只替换事件入口，不替换主数据回查端口、纯核心、幂等日志和同步计划。

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
| 联调 | 使用客户样例 `BaseOssDept` / `BaseOssUser` 事件验证接收、按 ID 回查、部门新增、人员新增、人员转部门、人员停用、未知角色 |

## 风险与决策

| 风险 | 等级 | 建议 |
|---|---|---|
| YAPI 未提供事件与主数据回查字段 | 高 | 不进入业务落库前必须补齐事件字段、用户接口、部门接口样例与字段字典 |
| SDK jar 当前缺失 | 中 | 第一版使用 HTTP 入口完成端口适配、测试和灾备；拿到 jar 后只补 SDK adapter |
| 事件无签名，仅 IP 白名单 | 中 | 至少增加来源应用白名单、trace 记录、可选共享密钥 |
| 用户邮箱唯一约束与客户字段不匹配 | 高 | 设计 external_user_id，并评估用户表唯一约束迁移 |
| 部门变更影响项目数据权限 | 高 | 先更新组织归属，不自动扩大项目可见范围 |
| 全量与增量并存导致乱序 | 中 | 使用事件时间、版本号或最后更新时间做冲突解决 |

## 待客户确认问题

1. `ClientSDK` jar 如何获取？是否提供 Maven 私服、账号、版本号和离线包？
2. YAPI 中 `BaseOssDept`、`BaseOssUser` 的完整字段、样例和错误码是什么？
3. 部门主数据回查接口、用户主数据回查接口的地址、入参、返回字段、分页和错误码是什么？
4. 生产事件库地址、主数据接口生产地址、测试地址、灾备地址是什么？
5. IP 白名单、HTTPS 证书、鉴权方式、签名方式、token 获取与过期策略是什么？
6. 人员唯一标识是什么？`userId` 是否稳定，是否可能合并或变更？
7. 邮箱、手机号是否必填且唯一？
8. 部门是否存在排序、区域、成本中心、业务线等扩展字段？
9. 角色/岗位如何映射到平台 `admin/manager/staff` 与自定义 `RoleProfile`？
10. 是否要求平台向客户系统回传同步结果事件？
