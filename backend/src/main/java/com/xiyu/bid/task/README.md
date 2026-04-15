# Task 模块 (任务协同模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
任务模块负责项目任务的查询、流转与协同，是项目执行过程中的任务拆解和进度跟踪后端入口。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/TaskController.java` | Controller | 任务接口 |
| `service/TaskService.java` | Service | 任务业务编排 |
| `dto/TaskDTO.java` | DTO | 任务视图对象 |
