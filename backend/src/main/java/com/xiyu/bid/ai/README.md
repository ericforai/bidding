# AI Service Module

> 一旦我所属的文件夹有所变化，请更新我。

## 目录功能

AI 智能分析服务模块，提供标讯分析和项目分析能力，支持多种 AI 提供商接入。

## 文件清单

### Client (AI 提供商)
- `client/AiProvider.java` - AI 服务提供者接口
- `client/MockAiProvider.java` - 模拟 AI 提供者（默认）
- `client/OpenAiProvider.java` - OpenAI 真实接入

### Service (业务层)
- `service/AiService.java` - AI 分析服务，异步处理标讯/项目分析

### DTO (数据传输对象)
- `dto/AiAnalysisResponse.java` - AI 分析响应
- `dto/DimensionScore.java` - 维度评分
- `dto/ProjectAnalysisDTO.java` - 项目分析结果

### Config (配置)
- `config/AsyncConfiguration.java` - 异步执行配置

## API 集成

- 标讯分析：`POST /api/tenders/{id}/analyze`
- 项目分析：通过 TenderService 调用

## 配置项

```yaml
ai:
  provider: ${AI_PROVIDER:mock}  # mock | openai
```
