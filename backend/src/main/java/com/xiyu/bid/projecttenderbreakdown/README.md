# 项目级招标文件拆解模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责说明

`projecttenderbreakdown` 承载项目详情页“解析招标文件”的项目级入口。
它负责权限守卫、解析就绪检查和 HTTP 边界编排，并把上传文件交给现有招标文件导入服务写入
`bid_tender_document_snapshots` 与 `bid_requirement_items`。

本模块保持 API 路径 `/api/projects/{projectId}/tender-breakdown` 不变。解析结果后续可被任务拆解和
AI 生成初稿共同复用，因此不再归属于 `biddraftagent` 的 controller/application 边界。

## 边界清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/` | API 边界 | 暴露项目级招标文件解析和 readiness 检查接口 |
| `application/` | Application Service / Port | 执行项目权限守卫、返回解析配置就绪状态 |

## 复用关系

- 上传解析仍复用 `BidTenderDocumentImportAppService.parseTenderDocument()`，避免复制文件保存、正文提取、需求项入库逻辑。
- DeepSeek 配置检查复用 `biddraftagent.application.TenderIntakeConfigurationReadiness` 端口和 readiness DTO，避免 AI 基础设施反向依赖本项目入口模块。
- 模块只做项目级入口编排，不承担招标要求抽取规则、任务生成规则或数据库实体转换。
