# ProjectWorkflow 模块 (项目流程衍生对象)

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
项目流程模块承接项目任务、提醒、分享链接、文档和评分草稿等派生能力。它位于 Project 域的执行侧，负责把项目执行中的子流程收拢到统一边界。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/ProjectWorkflowController.java` | Controller | 项目流程接口 |
| `service/ProjectWorkflowService.java` | Service | 项目流程编排；评分草稿更新和任务生成规则先形成无 I/O 决策，再由事务外壳保存 |
| `service/ScoreDraftParserService.java` | Service | 评分草稿解析 |
| `entity/ProjectDocument.java` | Entity | 项目文档实体 |
| `entity/ProjectReminder.java` | Entity | 项目提醒实体 |
| `entity/ProjectShareLink.java` | Entity | 项目分享链接实体 |
| `entity/ProjectScoreDraft.java` | Entity | 评分草稿实体 |
| `repository/` | Repository | 项目流程持久化访问 |
| `dto/` | DTO | 任务、提醒、文档、分享和评分草稿请求/响应 |
