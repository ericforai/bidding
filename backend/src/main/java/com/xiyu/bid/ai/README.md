# AI 模块

> 一旦我所属的文件夹有所变化，请更新我。

## 职责说明
AI 分析能力的后端入口，负责标讯分析、项目分析与 AI Provider 适配。
该模块只负责能力编排，不承载业务主数据或页面状态。
真实接入与 mock 接入通过 Provider 抽象切换。

## 边界清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `client/` | 子目录 | AI 提供商接入边界 |
| `client/AiProvider.java` | Interface | AI 提供者统一抽象 |
| `client/MockAiProvider.java` | Implementation | Mock AI 提供者 |
| `client/OpenAiProvider.java` | Implementation | OpenAI 真实接入实现 |
| `service/` | 子目录 | AI 分析编排边界 |
| `service/AiService.java` | Service | 标讯/项目分析服务 |
| `dto/` | 子目录 | AI 响应与评分边界 |
| `dto/AiAnalysisResponse.java` | DTO | AI 分析响应 |
| `dto/DimensionScore.java` | DTO | 维度评分结果 |
| `dto/ProjectAnalysisDTO.java` | DTO | 项目分析结果 |
| `config/` | 子目录 | AI 异步执行配置边界 |
| `config/AsyncConfiguration.java` | Config | 异步执行配置 |
