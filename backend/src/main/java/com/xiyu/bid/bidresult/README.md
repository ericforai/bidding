# BidResult 模块 (投标结果闭环模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
投标结果闭环模块负责：
1. 手工登记中标/落标 + 合同期限（含 SKU/金额/备注）
2. 内部 ERP/CRM 同步 + 公开信息抓取 + 人工确认
3. 销售上传中标通知书/分析报告提醒
4. 竞争对手中标记录（SKU/品类/折扣/账期）及聚合报表

## Split-First 服务拆分
原 `BidResultService`（365 行上帝类）已拆为 5 个单一职责服务：

| 服务 | 职责 | 依赖数 |
|------|------|--------|
| `BidResultQueryService` | 只读 overview/详情/列表 | 5 |
| `BidResultRegistrationService` | 手工登记 / 补录 / 确认 / 忽略 | 3 |
| `BidResultReminderService` | 提醒创建 / 发送 / 批量发送 | 4 |
| `BidResultSyncService` | 内部同步 / 公开抓取 + 同步日志 | 4 |
| `CompetitorReportService` | 竞争对手中标登记 + 聚合报表 | 3 |

## 纯核心 (FP-Java Profile)
`core/` 下为 Record/无状态静态方法，无 Spring / JPA / IO：

| 文件 | 职责 |
|------|------|
| `AwardRegistration` | 登记表单纯值对象 |
| `AwardRegistrationValidation` | 登记校验规则（返回 ValidationResult） |
| `CompetitorWinRow` | 报表输入单行 |
| `CompetitorReportRow` | 报表输出单行 |
| `CompetitorReportComputation` | 聚合算法（SKU sum / 品类众数 / 折扣均值 / 趋势） |

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/BidResultController.java` | Controller | HTTP 派发，不做业务决策 |
| `service/BidResultQueryService.java` | Service | 只读查询聚合 |
| `service/BidResultRegistrationService.java` | Service | 手工登记 + 补录 + 确认/忽略 |
| `service/BidResultReminderService.java` | Service | 提醒写入 + 发送 |
| `service/BidResultSyncService.java` | Service | 同步/抓取 + 日志 |
| `service/CompetitorReportService.java` | Service | 竞争对手中标登记 + 报表 |
| `core/*` | Pure Core | 校验 + 聚合纯函数（无框架依赖） |
| `entity/BidResultFetchResult.java` | Entity | 抓取/同步/手工登记结果实体 |
| `entity/BidResultReminder.java` | Entity | 上传提醒记录实体 |
| `entity/BidResultSyncLog.java` | Entity | 同步/抓取操作日志实体 |
| `entity/CompetitorWinRecord.java` | Entity | 竞争对手中标记录实体 |
| `repository/BidResultFetchResultRepository.java` | Repository | 结果记录数据访问 |
| `repository/BidResultReminderRepository.java` | Repository | 提醒记录数据访问 |
| `repository/BidResultSyncLogRepository.java` | Repository | 同步日志数据访问 |
| `repository/CompetitorWinRecordRepository.java` | Repository | 竞争对手中标记录访问 |
| `dto/BidResultAssembler.java` | Assembler | Entity→DTO 单一装配 |
| `dto/CompetitorReportAssembler.java` | Assembler | 竞争对手 Entity/Core→DTO 装配 |
| `dto/BidResultOverviewDTO.java` | DTO | 概览卡片视图对象 |
| `dto/BidResultFetchResultDTO.java` | DTO | 待确认结果视图对象（含合同期限/备注/SKU） |
| `dto/BidResultReminderDTO.java` | DTO | 提醒记录视图对象 |
| `dto/BidResultCompetitorReportRowDTO.java` | DTO | 竞争对手报表行对象 |
| `dto/BidResultDetailDTO.java` | DTO | 结果详情视图对象 |
| `dto/BidResultActionRequest.java` | DTO | 闭环动作请求对象 |
| `dto/BidResultSyncResponseDTO.java` | DTO | 同步/抓取响应对象 |
| `dto/BidResultRegisterRequest.java` | DTO | 手工登记请求 |
| `dto/BidResultUpdateRequest.java` | DTO | 登记补录请求 |
| `dto/CompetitorWinRequest.java` | DTO | 竞争对手中标登记请求 |
| `dto/CompetitorWinDTO.java` | DTO | 竞争对手中标记录视图对象 |

## 数据迁移
- `V50__create_bid_result_tables.sql` — 原始三表
- `V61__bid_result_closure_enhancement.sql` — 补齐合同期限/备注/SKU/附件链接字段 + 竞争对手中标记录表
