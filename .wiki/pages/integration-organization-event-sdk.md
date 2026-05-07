---
title: 组织架构对接 - 客户事件库 SDK 方案
space: engineering
category: integration
tags: [integration, organization, event-sdk, event-bus, data-scope]
sources:
  - .wiki/sources/customer/事件库SDK接入说明方案.doc
  - backend/src/main/java/com/xiyu/bid/entity/User.java
  - backend/src/main/java/com/xiyu/bid/dto/DataScopeConfigPayload.java
backlinks:
  - _index
  - integration-oa-crm
created: 2026-04-28
updated: 2026-05-07
health_checked: 2026-05-07
---
# 组织架构对接 - 客户事件库 SDK 方案

## 结论

客户最新 V1.0 技术文档明确：组织架构同步采用“事件库 SDK 订阅 + 组织架构接口查询”模式。西域数智化投标管理平台（以下简称“投标系统”）应将事件作为“变更通知”和“触发器”，通过回查主数据接口获取最新、可信的组织架构数据。

- **事件消息只作为触发器**：不得直接将事件 payload 中的 `data` 当作主数据使用。
- **最终数据源**：以组织架构接口返回结果为准，按 `userId`、`deptId` 等唯一标识执行本地 `upsert`。
- **幂等处理**：系统必须具备幂等处理能力，防止重复消费或短时间多次变更导致的数据不一致。
- **初始化与对账**：上线前全量初始化，上线后增量同步，并保留定时对账机制。

## 客户接入规范摘要

| 项 | 客户文档要求 (V1.0) |
|---|---|
| Java SDK 坐标 | `com.ehsy.eventlibrary:ClientSDK:${eventlibrary.version}`，当前版本 `release_0.0.2` |
| 注册配置 | `client.register.serviceName`、`serverRegisterUrl`、`enableRegister` |
| 续约配置 | `client.renewal.initialDelay`、`period`、`renewalDuration` |
| 消费事件 | 方法增加 `@AcceptEvent(eventTopic = "...", consumerGroup = "...")`，入参为 `String eventMessage` |
| 响应约定 | 处理成功返回 `EventResult` code=200；重试返回 code=500 |

## 接入范围与事件契约

| 数据类别 | Topic | 关键唯一标识 | 后续动作 |
|---|---|---|---|
| 部门信息 | `BaseOssDept` | `deptId` / `key` | 调用“根据部门编码获取部门数据”接口 |
| 员工信息 | `BaseOssUser` | `userId` / `key` | 调用“根据员工 ID 获取员工数据”接口 |

### 公共消息结构

| 字段 | 说明 |
|---|---|
| `traceId` | 事件链路追踪 ID，用于问题定位 |
| `spanId` | 事件链路 spanId |
| `eventSource` | 事件来源系统 (当前为 `oss`) |
| `eventTopic` | 事件主题 (`BaseOssDept`, `BaseOssUser`) |
| `time` | 事件产生时间 (毫秒时间戳) |
| `key` | 业务 key (`deptId` 或 `userId`) |
| `data` | 数据载体，仅包含关键标识 |

## 标准处理流程

1. **SDK 初始化**：服务启动后完成注册、续约。
2. **事件推送**：接收 `BaseOssDept` 或 `BaseOssUser` 事件消息。
3. **解析路由**：解析 JSON 字符串，根据 `eventTopic` 路由到对应处理器。
4. **日志流水**：记录 `traceId`、`eventTopic`、`key`、`time` 等日志。
5. **接口回查**：调用西域组织架构接口获取最新完整数据。
6. **本地更新**：按业务主键 `upsert` 到本地表；如状态失效，执行禁用/离职处理。
7. **反馈结果**：返回处理成功或失败标识。

## 对平台现状的映射

| 客户组织概念 | 平台落点 | 处理策略 |
|---|---|---|
| 部门 ID | `deptId` (业务主键) | 不得使用自增 ID，基于 `deptId` 进行 upsert |
| 员工 ID | `userId` (业务主键) | 不得使用自增 ID，基于 `userId` 进行 upsert |
| 启停用/状态 | `enabled` 字段 | 接口未查询到或状态失效时禁用，不得物理删除 |

## 推荐后端模块拆分

```text
backend/src/main/java/com/xiyu/bid/integration/organization/
├── domain/                  # 纯核心：事件解析、同步计划、差异策略
├── application/             # 事务编排：主数据回查、保存、审计
├── sdk/                     # SDK 适配：@AcceptEvent 消费
├── controller/              # 灾备入口：Webhook Webhook 
└── infrastructure/          # 持久化：部门/人员 Entity 与 Repository
```

## 异常处理与重试

- **超时设置**：建议 3-5 秒，失败后进入指数退避重试.
- **格式异常**：记录失败并进入人工处理队列.
- **并发控制**：同一对象建议串行或分布式锁，避免覆盖.

## 验证与对账

- **TC-01 SDK 启动**：验证服务注册与处理器加载。
- **TC-02 数据变更**：验证从事件触发到本地库更新的闭环。
- **TC-04 幂等验证**：重复投递不产生脏数据。
- **对账机制**：建议每天低峰期拉取最近 1-3 天数据进行对账修复。

## 相关文档

- [[architecture]] §5 API 集成层
- [[integration-oa-crm]] OA 与 CRM 集成规范
- `.wiki/sources/technical/西域投标管理平台与西域对接技术相关内容.md` (原始文档)
