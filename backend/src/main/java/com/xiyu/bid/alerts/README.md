# Alerts Module (告警模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

提供告警规则配置和告警历史记录功能，支持项目关键事件的定时监控和通知。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `AlertRule.java` | Entity | 告警规则实体 |
| `AlertHistory.java` | Entity | 告警历史记录实体 |
| `AlertRuleRepository.java` | Repository | 告警规则数据访问层 |
| `AlertHistoryRepository.java` | Repository | 告警历史数据访问层 |
| `AlertRuleService.java` | Service | 告警规则业务逻辑层 |
| `AlertHistoryService.java` | Service | 告警历史业务逻辑层 |
| `AlertSchedulerService.java` | Service | 定时任务调度服务 |
| `AlertRuleController.java` | Controller | 告警规则 API 端点 |
| `AlertHistoryController.java` | Controller | 告警历史 API 端点 |
| `AlertRuleCreateRequest.java` | DTO | 创建告警规则请求 |
| `AlertRuleUpdateRequest.java` | DTO | 更新告警规则请求 |
| `AlertHistoryCreateRequest.java` | DTO | 创建告警历史请求 |
| `AlertStatisticsResponse.java` | DTO | 告警统计数据响应 |

## 告警类型

| 类型 | 描述 | 触发条件 |
|------|------|----------|
| DEADLINE | 截止日期提醒 | 项目临近截止 |
| FEE_PENDING | 费用待支付 | 费用状态为 PENDING 且超期 |
| COMPLIANCE | 合规检查失败 | 合规检查发现问题 |
| DOCUMENT_MISSING | 文档缺失 | 必需文档未上传 |

## API 端点

### 告警规则
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/alerts/rules` | 获取告警规则列表 |
| POST | `/api/alerts/rules` | 创建告警规则 |
| PUT | `/api/alerts/rules/{id}` | 更新告警规则 |
| DELETE | `/api/alerts/rules/{id}` | 删除告警规则 |
| GET | `/api/alerts/statistics` | 获取告警统计 |

### 告警历史
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/alerts/history` | 获取告警历史 |
| POST | `/api/alerts/history` | 记录告警 |
| PUT | `/api/alerts/history/{id}/resolve` | 标记已解决 |
