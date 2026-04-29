# integration 模块（系统集成）

> 一旦我所属的文件夹有所变化，请更新我。

## 职责

负责第三方系统集成的领域逻辑、凭证管理、连通性探测和客户事件接入。当前支持企业微信（WeCom）集成，并新增客户组织架构事件库 HTTP 中转接入；泛微 OA 流程触发与回调验签的第一版适配位于 `../workflowform/infrastructure/oa`，由流程表单中心持有用例边界，详见 `docs/WORKFLOW_FORM_CENTER.md` 和 `.wiki/pages/workflow-form-center.md`。遵循六边形架构：domain 定义纯业务规则，application 编排用例，infrastructure 实现外部 I/O，controller 暴露 HTTP 接口。

## 目录结构

```
integration/
├── application/          # 用例编排：连通性探测、凭证加密
│   ├── WeComConnectivityProbe.java
│   ├── WeComCredentialCipher.java
│   └── WeComMockConnectivityProbe.java
├── controller/           # HTTP 控制器（/api/admin/integrations/wecom）
├── domain/               # 纯业务模型，无框架依赖
│   ├── ValidationResult.java
│   ├── WeComConnectivityResult.java
│   ├── WeComCredential.java
│   └── WeComCredentialValidation.java
├── dto/                  # 请求/响应 DTO
│   ├── WeComConnectivityResponse.java
│   ├── WeComIntegrationRequest.java
│   └── WeComIntegrationResponse.java
└── infrastructure/       # JPA 持久化实现
    └── persistence/
        ├── entity/WeComIntegrationEntity.java
        └── repository/WeComIntegrationJpaRepository.java
└── organization/         # 客户组织架构事件库接入
    ├── domain/           # 纯核心：事件校验、topic 分类、角色映射、用户同步计划
    ├── application/      # 应用编排：签名校验、幂等占位、用户/部门落库协调
    ├── controller/       # HTTP 中转入口（/api/integrations/organization/events）
    ├── dto/              # 客户 code/msg/timestamp/data 响应契约
    └── infrastructure/   # 组织部门与事件 inbox 持久化
```

## 组织架构事件接入口径

- 纯核心：`organization/domain` 只接收显式输入并返回显式结果，不读写数据库、时间、日志或外部 SDK。
- 副作用边界：`OrganizationEventWebhookController` 做 HTTP 头转换与 HMAC 签名校验；`OrganizationEventAppService` 做事务编排；`OrganizationEventLogRetentionService` 做事件日志保留清理；JPA repository 负责事件 inbox、部门和用户状态写入。
- 安全边界：webhook 路径虽允许机器请求绕过用户 JWT，但必须携带 `EHSY-TraceID`、`EHSY-SRCAPP`、`EHSY-Signature`；签名使用 `xiyu.integrations.organization.webhook-secret` 做 HMAC-SHA256 校验。
- 启停开关：`xiyu.integrations.organization.enabled=false` 时，签名通过的事件也会被拒绝并记录为 `REJECTED`，便于生产紧急止血。
- 幂等策略：事件进入业务处理前先写入 `organization_event_logs` 的 `PROCESSING` 占位；重复事件稳定返回成功且标记 duplicate。
- 角色策略：未知外部角色默认降级为 `staff`；只有显式配置 allowlist 的外部角色编码才会映射到 `manager/admin`，避免 webhook 自动提权。
- 保留策略：`xiyu.integrations.organization.event-log-retention-days` 默认 90 天，定时任务按 `received_at` 清理过期事件日志；配置为 `0` 或负数可暂停清理。

## 泛微 OA 接入口径

- 泛微 OA 不放在通用 `integration` 用例层直接驱动业务，而是由 `workflowform` 模块通过 `OaWorkflowGateway` 端口发起流程。
- `MockOaWorkflowGateway` 只作为受 profile/config 控制的联调占位；真实泛微 HTTP 适配器保留在 `WeaverOaWorkflowGateway`，等待客户接口资料补齐。
- 回调入口为 `/api/integrations/oa/weaver/callback`，由 `OaCallbackVerifier` 做 secret、时间窗和签名校验，再交给流程表单应用服务做幂等和业务应用。
- 审批事实源是 OA，本系统只保存表单实例、OA 实例号、审批结果和业务应用状态。
