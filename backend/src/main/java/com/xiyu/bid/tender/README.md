# Tender 模块 (标讯主数据模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
标讯模块负责标讯数据的查询、创建与更新，是投标机会识别和标讯主列表的后端入口。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/TenderController.java` | Controller | 标讯接口 |
| `service/TenderService.java` | Service | 标讯业务编排 |
| `dto/TenderDTO.java` | DTO | 标讯视图对象 |
| `dto/TenderRequest.java` | DTO | 标讯创建/更新请求 |
