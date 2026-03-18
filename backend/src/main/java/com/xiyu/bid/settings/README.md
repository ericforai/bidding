# settings 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
提供系统设置的最小可用后端能力，覆盖系统配置、角色权限、部门数据权限、项目组可见范围以及关键集成配置的读取与更新。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/SettingsController.java` | Controller | 暴露 `/api/settings` 的读取与更新接口 |
| `service/SettingsService.java` | Service | 聚合设置数据并处理更新逻辑 |
| `dto/SettingsResponse.java` | DTO | 返回系统设置聚合视图 |
| `dto/SettingsUpdateRequest.java` | DTO | 承载系统设置更新请求 |

## 当前边界
- 仅处理系统治理相关配置，不承载用户认证、审计明细或业务主数据。
- 当前实现以最小可用闭环为目标，优先服务验收整改与 API 模式联调。

## 后续建议
- 将内存态配置迁移到持久化存储。
- 为用户级数据权限补充独立契约与测试。
- 增加审计日志，记录设置变更人、时间和变更内容。
