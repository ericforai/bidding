# 标书生成 Agent 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责说明

`biddraftagent` 是项目详情里的“AI 生成标书初稿”后端主链路。
它负责把上传的招标文件、项目、标讯和企业资料快照拆解成可写作草稿、缺漏检查、人工确认提示，并在 `apply` 时写入 `documenteditor` 的章节树。

本模块不是“自动中标/自动定稿”系统，报价、法务承诺、资质真实性和关键商务偏离必须保留人工确认提示。

## 边界清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `domain/` | 纯核心 | 招标要求归类、材料匹配打分、缺漏检查、人工确认和写作覆盖决策 |
| `application/` | Application Service / Port / Planner | 编排招标文件导入、run 生命周期、生成写入计划、定义文档写入端口 |
| `infrastructure/documenteditor/` | Adapter | 把写入计划转换为 `documenteditor` 批量章节树写入请求 |
| `infrastructure/openai/` | Adapter | 通过 OpenAI Java SDK + Responses API structured outputs 拆解招标要求、生成草稿、审阅摘要和交接清单 |
| `infrastructure/tenderdocument/` | Adapter | 保存上传文件，使用 POI/PDFBox 提取 Word 和文本型 PDF 正文 |
| `controller/` | API 边界 | 暴露项目级 tender document import、run/review/apply 接口 |
| `repository/` | JPA Repository | 读写 run、artifact、招标文件解析快照与 requirement items |
| `entity/` | JPA Entity | `bid_agent_runs`、`bid_agent_artifacts`、`bid_tender_document_snapshots`、`bid_requirement_items` |
| `dto/` | DTO | API 入出参和前端展示 payload |

## 纯核心边界

- `domain/*` 受 `FPJavaArchitectureTest` 保护。
- `domain/*` 不依赖 Spring、Repository、JPA、日志、IO、异常业务流、时间或随机数。
- 应用层只负责编排：取快照、调用纯核心、生成 artifact、写 run 状态、调用文档写入端口。
- `documenteditor` 写入只发生在基础设施适配器中，且必须尊重锁定章节和来源 metadata。
- 招标文件结构化拆解和草稿正文生成只有 OpenAI 真实调用路径；API key 优先读取 `ai.openai.api-key`，缺省时读取系统设置 `integrationConfig.apiKey`。baseUrl/model 同理可通过 `ai.openai.*` 或系统设置 `integrationConfig.aiBaseUrl`、`integrationConfig.aiModel` 配置；未配置有效 key 时应显式失败，不回落到模板或 mock 生成。

## 招标文件到标书初稿链路

1. `POST /api/projects/{projectId}/bid-agent/tender-documents` 上传 `.doc`、`.docx` 或文本型 `.pdf` 招标文件。
2. `infrastructure/tenderdocument` 保存文件并提取正文；扫描件 PDF 会显式提示需要 OCR/人工处理。
3. `infrastructure/openai` 使用 structured outputs 拆解项目名称、招标范围、资格要求、技术要求、商务要求、评分标准、截止时间、必须材料和风险点。
4. 解析结果写入 `bid_tender_document_snapshots`、`bid_requirement_items`，并更新项目绑定的 Tender 描述和标签。
5. `BidDraftSnapshotAssembler` 把结构化 requirement items 纳入生成快照，再生成 run。
6. 默认 `applyToEditor=true`，生成后立即写入 `documenteditor` 章节树；锁定章节仍由 `documenteditor` 跳过。

## 关键 API

- `POST /api/projects/{projectId}/bid-agent/tender-documents`
- `POST /api/projects/{projectId}/bid-agent/runs`
- `GET /api/projects/{projectId}/bid-agent/runs/{runId}`
- `POST /api/projects/{projectId}/bid-agent/runs/{runId}/apply`
- `POST /api/projects/{projectId}/bid-agent/runs/{runId}/reviews`
- `POST /api/projects/{projectId}/bid-agent/reviews`
