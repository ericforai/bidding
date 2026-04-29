# workflowform 模块（流程表单中心）

一旦我所属的文件夹有所变化，请更新我。

## 职责

流程表单中心负责“表单模板读取、表单实例提交、触发 OA、接收 OA 结果、审批通过后应用业务动作”。本模块不实现本地审批流，审批事实源来自 OA；本系统只保存表单快照、OA 关联号、审批结果和业务应用状态。

## 边界

- `domain/` 是纯核心：表单 schema 校验、提交值校验、状态流转策略、OA 结果是否允许应用业务。
- `application/` 是编排层：提交表单、启动 OA、处理 OA 回调、调用业务应用端口。
- `infrastructure/` 是副作用层：JPA 持久化、OA Gateway 占位实现、资质借阅适配、项目权限守卫。
- `controller/` 和 `dto/` 只负责 HTTP 边界转换，不承载业务规则。

## 文件清单

| 文件 | 功能 |
|------|------|
| `domain/` | 表单 schema、提交值、状态流转和 OA 结果应用策略 |
| `application/` | 提交表单、触发 OA、处理回调、应用业务动作的用例编排 |
| `controller/` | 流程表单 HTTP 接口和泛微 OA 回调入口 |
| `dto/` | HTTP 请求和响应对象 |
| `infrastructure/` | JPA 持久化、OA Gateway、项目权限守卫和资质借阅适配 |

## 资质借阅

资质借阅表单提交后状态进入 `OA_APPROVING`，不会立即借出。只有 OA 回调为 `APPROVED` 且状态仍允许流转时，才通过 `QualificationBorrowApplyPort` 调用现有资质借阅应用服务；OA 驳回、重复回调或业务应用失败都不会重复创建借阅记录。
