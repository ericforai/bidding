一旦我所属的文件夹有所变化，请更新我。

# Approval 模块

该目录负责审批请求、审批动作和审批统计，是投标流程中的统一审批域。
当前 README 先覆盖模块职责和关键文件，后续再逐步补齐完整清单表格。

| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/ApprovalController.java` | Controller | 提供审批详情、提交、决策和统计接口 |
| `service/ApprovalWorkflowService.java` | Service | 承接审批流编排和状态流转 |
| `entity/ApprovalRequest.java` | Entity | 审批请求聚合实体 |
| `entity/ApprovalAction.java` | Entity | 审批动作记录实体 |
| `repository/ApprovalRequestRepository.java` | Repository | 审批请求数据访问 |
| `repository/ApprovalActionRepository.java` | Repository | 审批动作数据访问 |
