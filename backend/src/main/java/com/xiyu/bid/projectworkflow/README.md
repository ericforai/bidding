# ProjectWorkflow 模块 (项目流程衍生对象)

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
项目流程模块承接项目任务、提醒、分享链接、文档和评分草稿等派生能力。它位于 Project 域的执行侧，负责把项目执行中的子流程收拢到统一边界。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/ProjectWorkflowController.java` | Controller | 项目流程接口 |
| `controller/ProjectDocumentController.java` | Controller | 项目文档接口，承接结果附件查询/创建/删除 |
| `core/ScoreDraftPolicy.java` | Core | 评分草稿更新与任务生成的纯规则，返回显式决策和值对象，不直接做 I/O |
| `core/TaskBreakdownPolicy.java` | Core | 根据招标需求项或标书章节快照生成任务拆解决策，不访问数据库或框架 |
| `service/ProjectWorkflowService.java` | Service | 项目流程门面；转调任务、评分草稿、文档、提醒和分享链接子服务 |
| `service/ProjectTaskBreakdownService.java` | Service | 任务拆解编排；读取真实标书拆解结果、调用纯核心、保存项目任务 |
| `service/ProjectTaskBreakdownSourceReader.java` | Reader | 读取任务拆解来源；招标需求项复用 `biddraftagent` 的最新快照读模型，章节只兜底读取顶层/二级章节 |
| `service/ProjectDocumentWorkflowService.java` | Service | 项目文档查询/创建/删除编排，调用文档绑定边界 |
| `service/ProjectDocumentViewAssembler.java` | Assembler | 项目文档实体到 DTO 的装配 |
| `service/ProjectDocumentBindingGateway.java` | Port | 项目文档与外部附件业务的可替换集成边界 |
| `service/ScoreDraftParserService.java` | Service | 评分草稿解析 |
| `entity/ProjectDocument.java` | Entity | 项目文档实体 |
| `entity/ProjectReminder.java` | Entity | 项目提醒实体 |
| `entity/ProjectShareLink.java` | Entity | 项目分享链接实体 |
| `entity/ProjectScoreDraft.java` | Entity | 评分草稿实体 |
| `repository/` | Repository | 项目流程持久化访问 |
| `dto/` | DTO | 任务、提醒、文档、分享和评分草稿请求/响应 |
