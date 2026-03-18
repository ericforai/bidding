一旦我所属的文件夹有所变化，请更新我。

# Document Export 模块

该目录负责文档导出、归档和导出文件记录，是文档生命周期的输出侧模块。
当前 README 先覆盖模块职责和关键文件，后续再逐步补齐完整清单表格。

| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/DocumentExportController.java` | Controller | 暴露文档导出与归档接口 |
| `service/DocumentExportService.java` | Service | 执行文档导出和归档流程 |
| `entity/DocumentExport.java` | Entity | 导出任务实体 |
| `entity/DocumentExportFile.java` | Entity | 导出文件记录实体 |
| `entity/DocumentArchiveRecord.java` | Entity | 文档归档记录实体 |
| `repository/DocumentExportRepository.java` | Repository | 导出任务数据访问 |
