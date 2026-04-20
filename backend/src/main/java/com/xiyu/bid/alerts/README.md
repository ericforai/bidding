# Alerts 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责说明
告警模块负责规则配置、历史记录和定时调度，支撑项目关键事件的提醒与追踪。
规则与历史分层管理，调度逻辑负责把项目、标讯、资质、保证金退还跟踪等真实数据源映射为提醒。
历史记录在未解决态下按 `ruleId + relatedId` 去重，避免同一对象重复刷提醒。
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
| `AlertRuleExecutionService.java` | Service | 规则执行分发边界，包含 `DEPOSIT_RETURN` 委托 |
| `AlertRuleController.java` | Controller | 告警规则 API 边界 |
| `AlertHistoryController.java` | Controller | 告警历史 API 边界 |
| `AlertRuleCreateRequest.java` | DTO | 创建告警规则请求 |
| `AlertRuleUpdateRequest.java` | DTO | 更新告警规则请求 |
| `AlertHistoryCreateRequest.java` | DTO | 创建告警历史请求 |
| `AlertStatisticsResponse.java` | DTO | 告警统计响应 |
