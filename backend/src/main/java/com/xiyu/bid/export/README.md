一旦我所属的文件夹有所变化，请更新我。

# Export 模块

该目录负责通用导出任务和 Excel 导出服务，支撑多业务域的统一导出能力。
当前 README 先覆盖模块职责和关键文件，后续再逐步补齐完整清单表格。

| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/ExportController.java` | Controller | 暴露导出任务接口 |
| `service/ExcelExportService.java` | Service | 生成 Excel 导出内容 |
| `entity/ExportTask.java` | Entity | 导出任务实体 |
| `repository/ExportTaskRepository.java` | Repository | 导出任务数据访问 |
| `dto/ExportRequest.java` | DTO | 导出请求参数 |
| `dto/ExportResponse.java` | DTO | 导出响应结果 |
