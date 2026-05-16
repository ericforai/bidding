# Organization Event Sync 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
- 接收西域组织架构变更事件（SDK 订阅 + HTTP 灾备双路）。
- 实现事件层幂等（traceId+spanId+eventTopic）与业务主键 upsert。
- 提供全量初始化与日常对账能力。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `config/OrganizationSyncProperties.java` | Config | SDK/HTTP/对账配置参数 |
| `domain/EventDeduplicationPolicy.java` | Domain | 事件去重键生成 |
| `domain/OrganizationUpsertPolicy.java` | Domain | 创建/更新/删除决策规则 |
| `domain/ReconciliationPolicy.java` | Domain | 对账差异分类枚举 |
| `domain/EventValidationException.java` | Domain | 事件校验异常 |
| `application/EventSyncService.java` | Service | HTTP 灾备事件接收与入库 |
| `application/FullInitService.java` | Service | 全量初始化编排 |
| `application/ReconciliationService.java` | Service | 日常对账编排 |
| `infrastructure/HttpFallbackController.java` | Controller | HTTP 灾备 REST 入口 |
| `infrastructure/OrganizationController.java` | Controller | init/reconcile REST 入口 |
| `infrastructure/ClientSdkAdapter.java` | Adapter | SDK 订阅适配器（待 SDK jar） |
| `infrastructure/XiyuOrganizationApiClient.java` | Client | 回查西域组织架构接口 |
| `infrastructure/persistence/entity/*` | Entity | Inbox/Department/User JPA 实体 |
| `infrastructure/persistence/repository/*` | Repository | JPA 数据访问接口 |
