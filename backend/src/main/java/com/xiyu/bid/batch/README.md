一旦我所属的文件夹有所变化，请更新我。

# Batch 模块

该目录负责批量任务操作，包括批量分配、认领和删除等后端编排能力。
当前 README 先覆盖模块职责和关键文件，后续再逐步补齐完整清单表格。

| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/BatchOperationController.java` | Controller | 暴露批量操作 API |
| `service/BatchOperationService.java` | Service | 执行批量分配、认领和删除逻辑 |
| `dto/BatchAssignRequest.java` | DTO | 批量分配请求 |
| `dto/BatchClaimRequest.java` | DTO | 批量认领请求 |
| `dto/BatchDeleteRequest.java` | DTO | 批量删除请求 |
| `dto/BatchOperationResponse.java` | DTO | 批量操作结果响应 |
