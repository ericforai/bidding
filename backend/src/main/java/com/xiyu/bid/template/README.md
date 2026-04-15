# Template 模块 (模板知识模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
模板模块负责模板列表、复制、下载与版本信息，是前端模板库与知识复用能力的后端支撑入口。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/TemplateController.java` | Controller | 模板接口 |
| `service/TemplateService.java` | Service | 模板业务编排 |
| `dto/TemplateDTO.java` | DTO | 模板视图对象 |
| `dto/TemplateVersionDTO.java` | DTO | 模板版本视图对象 |
| `dto/TemplateCopyRequest.java` | DTO | 模板复制请求 |
| `dto/TemplateDownloadRecordDTO.java` | DTO | 下载记录视图对象 |
| `dto/TemplateDownloadRecordRequest.java` | DTO | 下载记录请求 |
| `dto/TemplateUseRecordDTO.java` | DTO | 使用记录视图对象 |
| `dto/TemplateUseRecordRequest.java` | DTO | 使用记录请求 |
