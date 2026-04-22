# 标书生成 Agent 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责说明

`biddraftagent` 是项目详情里的“AI 生成标书初稿”后端主链路。
它负责把项目、标讯和企业资料快照拆解成可写作草稿、缺漏检查、人工确认提示，并在 `apply` 时写入 `documenteditor` 的章节树。

本模块不是“自动中标/自动定稿”系统，报价、法务承诺、资质真实性和关键商务偏离必须保留人工确认提示。

## 边界清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `domain/` | 纯核心 | 招标要求归类、材料匹配打分、缺漏检查、人工确认和写作覆盖决策 |
| `application/` | Application Service / Port / Planner | 编排 run 生命周期、生成写入计划、定义文档写入端口 |
| `infrastructure/documenteditor/` | Adapter | 把写入计划转换为 `documenteditor` 批量章节树写入请求 |
| `controller/` | API 边界 | 暴露项目级 bid-agent run/review/apply 接口 |
| `repository/` | JPA Repository | 读写 run 与 artifact 持久化 |
| `entity/` | JPA Entity | `bid_agent_runs`、`bid_agent_artifacts` |
| `dto/` | DTO | API 入出参和前端展示 payload |

## 纯核心边界

- `domain/*` 受 `FPJavaArchitectureTest` 保护。
- `domain/*` 不依赖 Spring、Repository、JPA、日志、IO、异常业务流、时间或随机数。
- 应用层只负责编排：取快照、调用纯核心、生成 artifact、写 run 状态、调用文档写入端口。
- `documenteditor` 写入只发生在基础设施适配器中，且必须尊重锁定章节和来源 metadata。

## 关键 API

- `POST /api/projects/{projectId}/bid-agent/runs`
- `GET /api/projects/{projectId}/bid-agent/runs/{runId}`
- `POST /api/projects/{projectId}/bid-agent/runs/{runId}/apply`
- `POST /api/projects/{projectId}/bid-agent/reviews`
