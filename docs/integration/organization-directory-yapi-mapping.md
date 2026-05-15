# 组织架构 YAPI 契约映射

本文冻结西域数智化投标管理平台与客户组织架构系统的对接口径。当前交付模式为真实 API 单一路径 (API-only)，不新增 Mock、不伪造 SDK、不编造 YAPI host、token 或测试账号。

## 范围

- In scope: 组织架构部门与员工主数据回查、时间窗同步、事件触发后的统一应用服务链路。
- Out of scope: OA 流程创建、CRM 客户接口、前端 demo/mock fallback。
- 当前状态: 客户 `ClientSDK` JAR 尚未交付，先使用统一应用服务链路承接 HTTP 中转、回查、幂等、落库和后续重试。拿到真实 SDK 后，只新增 SDK adapter，并委托现有应用服务，不复制业务规则。

## 事件与回查入口

| Event Topic | Identifier | Triggered Capability | Local Entry |
| --- | --- | --- | --- |
| `BaseOssDept` | `data.deptId` | Department detail | `OrganizationDirectorySyncAppService` |
| `BaseOssUser` | `data.userId` | User detail | `OrganizationDirectorySyncAppService` |

事件 `data` 只作为变更通知标识，不作为部门或员工主数据 payload。主数据必须通过 YAPI 详情接口回查后写入本地。

## YAPI 映射表

| Capability | Method | Path | Auth | Request Fields | Response Data Path | Local Mapping | Not Found Semantics |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Department detail | TBD | TBD | TBD | `deptId` | TBD | `deptId` -> `externalDeptId`, `deptName` -> `departmentName` | disable / pending-confirm |
| User detail | TBD | TBD | TBD | `userId` | TBD | `userId` -> `externalUserId`, `userNo` -> `username` | disable / pending-confirm |
| Department window | TBD | TBD | TBD | `startAt`, `endAt`, `pageNo`, `pageSize` | TBD | list of department snapshots | empty page ends sync |
| User window | TBD | TBD | TBD | `startAt`, `endAt`, `pageNo`, `pageSize` | TBD | list of user snapshots | empty page ends sync |

## 字段映射待冻结

| Domain | Remote Field | Local Field | Status | Notes |
| --- | --- | --- | --- | --- |
| Department | `deptId` | `externalDeptId` | Pending confirm | 作为幂等和手工重同步主键。 |
| Department | `deptName` | `departmentName` | Pending confirm | 需确认空值和重命名语义。 |
| Department | parent department field | parent external department reference | TBD | 字段名、根部门语义、跨层级移动语义待确认。 |
| Department | disabled/deleted/status field | local disabled state | TBD | 需与禁用/查无语义一起冻结。 |
| User | `userId` | `externalUserId` | Pending confirm | 作为幂等和手工重同步主键。 |
| User | `userNo` | `username` | Pending confirm | 需确认是否可作为登录名、是否全局唯一。 |
| User | department relation field | user department relation | TBD | 字段名、主部门/兼职部门语义待确认。 |
| User | role/title field | role mapping input | TBD | 未配置 allowlist 的外部角色不得自动提权。 |
| User | email/mobile | local contact fields | TBD | 需确认是否登录必需；日志和错误响应必须脱敏。 |
| User | disabled/deleted/status field | local disabled state | TBD | 需与禁用/查无语义一起冻结。 |

## Blocking Inputs

### ClientSDK

- Maven coordinates: `groupId` / `artifactId` / `version` 为 TBD。
- 私服地址或 JAR 交付流程为 TBD。
- `@AcceptEvent` 包名为 TBD。
- `EventResult` 基类或返回类型为 TBD。
- `consumerGroup` 命名规则为 TBD。
- `BaseOssDept`、`BaseOssUser` Topic 名称需由客户最终确认大小写和环境隔离规则。

### YAPI 鉴权 Header

- 鉴权 header 名称、token 格式、签名算法、token 生命周期为 TBD。
- source app header 名称和值为 TBD。
- trace header 名称为 TBD。
- timestamp / nonce 是否必填为 TBD。
- 测试与生产 host 分离规则为 TBD。
- 本文不记录真实 token、测试账号或私有 host。

### YAPI 响应与错误语义

- 成功码字段、成功码取值、data 路径为 TBD。
- 分页字段名、总数语义、空页终止规则为 TBD。
- 404、业务查无、禁用、删除、权限不足的响应差异为 TBD。
- 可重试错误码、不可重试合同错误、鉴权失败错误码为 TBD。

### 禁用/查无处理

- 详情接口查无时，本地记录是直接禁用还是进入 `pending-confirm` 为 TBD。
- 时间窗同步缺失某记录时，是否代表删除为 TBD。
- 员工离职、部门撤销、组织迁移的状态字段和回补时序为 TBD。

### 告警阈值

- 事件处理成功率阈值为 TBD。
- YAPI HTTP 失败率阈值为 TBD。
- YAPI P95/P99 延迟阈值为 TBD。
- pending retry 数量阈值为 TBD。
- dead letter 数量阈值为 TBD。
- 每日对账差异数量阈值为 TBD。
- 告警接收人、升级路径和工作时间/非工作时间策略为 TBD。

## SDK JAR 缺失口径

在客户交付真实 `ClientSDK` 前，禁止提交自造 SDK、占位注解或假 `EventResult`。当前实现和验收说明以统一应用服务链路为准:

1. HTTP 中转或测试入口接收事件通知。
2. 应用服务解析 `BaseOssDept` / `BaseOssUser` 事件标识。
3. 应用服务按 `deptId` / `userId` 调用组织架构主数据回查网关。
4. writer 负责部门、用户、角色映射和数据权限读模型落库。
5. 拿到真实 SDK 后，在 `infrastructure/sdk` 增加 adapter，把 SDK 回调转换为同一应用服务命令。
