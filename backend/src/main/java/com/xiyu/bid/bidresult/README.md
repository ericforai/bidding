# BidResult 模块 (投标结果闭环模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
投标结果闭环模块负责中标结果聚合、公开抓取待确认、上传提醒和竞争分析报表，是投标结果交付链路的统一聚合域。这里统一编排真实结果记录、提醒记录和同步操作日志。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/BidResultController.java` | Controller | 投标结果闭环接口 |
| `service/BidResultService.java` | Service | 结果聚合、确认、提醒和报表编排 |
| `entity/BidResultFetchResult.java` | Entity | 抓取/同步结果记录实体 |
| `entity/BidResultReminder.java` | Entity | 上传提醒记录实体 |
| `entity/BidResultSyncLog.java` | Entity | 同步/抓取操作日志实体 |
| `repository/BidResultFetchResultRepository.java` | Repository | 结果记录数据访问 |
| `repository/BidResultReminderRepository.java` | Repository | 提醒记录数据访问 |
| `repository/BidResultSyncLogRepository.java` | Repository | 同步日志数据访问 |
| `dto/BidResultOverviewDTO.java` | DTO | 概览卡片视图对象 |
| `dto/BidResultFetchResultDTO.java` | DTO | 待确认结果视图对象 |
| `dto/BidResultReminderDTO.java` | DTO | 提醒记录视图对象 |
| `dto/BidResultCompetitorReportRowDTO.java` | DTO | 竞争对手报表行对象 |
| `dto/BidResultDetailDTO.java` | DTO | 结果详情视图对象 |
| `dto/BidResultActionRequest.java` | DTO | 闭环动作请求对象 |
| `dto/BidResultSyncResponseDTO.java` | DTO | 同步/抓取响应对象 |
