# Task 模块 (任务协同模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
任务模块负责项目任务的查询、流转与协同，是项目执行过程中的任务拆解和进度跟踪后端入口。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/TaskController.java` | Controller | 任务接口 |
| `service/TaskService.java` | Service | 任务业务编排 |
| `service/TaskDeliverableService.java` | Service | 任务交付物元数据创建、删除和覆盖度查询；交付物 URL 来自真实项目文档上传结果 |
| `dto/TaskDTO.java` | DTO | 任务视图对象 |
| `dto/TaskDeliverableCreateRequest.java` | DTO | 任务交付物创建请求，包含名称、类型、文件信息和真实附件 URL |
