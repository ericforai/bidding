一旦我所属的文件夹有所变化，请更新我。

# Project Workflow 模块

该目录负责项目流程衍生对象，包括项目任务、提醒、分享链接、文档和评分草稿。
当前 README 先覆盖模块职责和关键文件，后续再逐步补齐完整清单表格。

| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/ProjectWorkflowController.java` | Controller | 暴露项目流程相关 API |
| `service/ProjectWorkflowService.java` | Service | 承接项目任务、提醒、分享和文档流程 |
| `service/ScoreDraftParserService.java` | Service | 解析评分草稿内容 |
| `dto/ProjectTaskViewDTO.java` | DTO | 项目任务视图对象 |
| `entity/ProjectDocument.java` | Entity | 项目文档实体 |
| `entity/ProjectReminder.java` | Entity | 项目提醒实体 |
