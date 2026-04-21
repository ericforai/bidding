# Workbench 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责说明
工作台模块负责工作台专属聚合查询接口，不承载底层日历、项目或审批的实体逻辑。
当前仅提供日程总览聚合，用于前端工作台展示。

## 边界清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/WorkbenchScheduleController.java` | Controller | 工作台日程总览 API |
