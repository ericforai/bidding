# Alerts 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责说明
告警模块负责规则配置、历史记录和定时调度，支撑项目关键事件的提醒与追踪。
规则与历史分层管理，调度逻辑只负责触发，不承载业务决策。
对外提供规则维护、历史查询和统计接口。

## 边界清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `AlertRule.java` | Entity | 告警规则实体 |
| `AlertHistory.java` | Entity | 告警历史记录实体 |
| `AlertRuleRepository.java` | Repository | 告警规则数据访问边界 |
| `AlertHistoryRepository.java` | Repository | 告警历史数据访问边界 |
| `AlertRuleService.java` | Service | 告警规则业务逻辑 |
| `AlertHistoryService.java` | Service | 告警历史业务逻辑 |
| `AlertSchedulerService.java` | Service | 定时触发与调度边界 |
| `AlertRuleController.java` | Controller | 告警规则 API 边界 |
| `AlertHistoryController.java` | Controller | 告警历史 API 边界 |
| `AlertRuleCreateRequest.java` | DTO | 创建告警规则请求 |
| `AlertRuleUpdateRequest.java` | DTO | 更新告警规则请求 |
| `AlertHistoryCreateRequest.java` | DTO | 创建告警历史请求 |
| `AlertStatisticsResponse.java` | DTO | 告警统计响应 |
