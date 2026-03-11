# Score Analysis Module (评分分析模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

对投标项目进行多维度评分分析，包括技术能力、财务实力、团队经验等维度，计算加权总分并评估风险等级。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `ScoreAnalysis.java` | Entity | 评分分析实体 |
| `DimensionScore.java` | Entity | 维度评分实体 |
| `RiskLevel.java` | Enum | 风险等级 (LOW, MEDIUM, HIGH, CRITICAL) |
| `ScoreAnalysisRepository.java` | Repository | 评分分析数据访问层 |
| `DimensionScoreRepository.java` | Repository | 维度评分数据访问层 |
| `ScoreAnalysisService.java` | Service | 评分分析业务逻辑层 |
| `ScoreAnalysisController.java` | Controller | REST API 端点 |
| `ScoreAnalysisDTO.java` | DTO | 评分分析数据传输对象 |
| `DimensionScoreDTO.java` | DTO | 维度评分数据传输对象 |
| `ScoreAnalysisCreateRequest.java` | DTO | 创建评分请求 |

## API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/ai/score-analysis` | 获取评分分析列表 |
| POST | `/api/ai/score-analysis` | 创建评分分析 |
| GET | `/api/ai/score-analysis/{id}` | 获取分析详情 |
| GET | `/api/ai/score-analysis/project/{projectId}` | 按项目查询 |

## 评分维度

| 维度 | 权重 | 说明 |
|------|------|------|
| 技术能力 | 25% | 技术方案、技术团队实力 |
| 财务实力 | 20% | 财务状况、资金能力 |
| 团队经验 | 20% | 类似项目经验 |
| 历史业绩 | 15% | 过往中标情况 |
| 合规性 | 10% | 资质、认证 |
| 价格竞争力 | 10% | 报价合理性 |

## 风险等级计算

- **LOW**: 总分 >= 80
- **MEDIUM**: 60 <= 总分 < 80
- **HIGH**: 40 <= 总分 < 60
- **CRITICAL**: 总分 < 40
