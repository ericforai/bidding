# 竞争情报模块 (CompetitionIntel Module)

## 概述

竞争情报模块提供竞争对手管理和竞争分析功能，帮助用户了解市场竞争态势，制定更有效的投标策略。

## 功能特性

- 竞争对手信息管理
- 竞争分析记录
- 胜率预测
- 历史表现跟踪
- 自动竞争分析

## 实体模型

### Competitor (竞争对手)

```java
@Entity
@Table(name = "competitors")
public class Competitor {
    private Long id;                      // 主键
    private String name;                  // 竞争对手名称（必填）
    private String industry;              // 所属行业
    private String strengths;             // 竞争优势
    private String weaknesses;            // 竞争劣势
    private BigDecimal marketShare;       // 市场份额（0-100）
    private BigDecimal typicalBidRangeMin; // 典型投标范围最小值
    private BigDecimal typicalBidRangeMax; // 典型投标范围最大值
    private LocalDateTime createdAt;      // 创建时间
}
```

### CompetitionAnalysis (竞争分析)

```java
@Entity
@Table(name = "competition_analyses")
public class CompetitionAnalysis {
    private Long id;                       // 主键
    private Long projectId;                // 项目ID（必填）
    private Long competitorId;             // 竞争对手ID（可选）
    private LocalDateTime analysisDate;    // 分析日期
    private BigDecimal winProbability;     // 胜率预测（0-100）
    private String competitiveAdvantage;   // 竞争优势分析
    private String recommendedStrategy;    // 建议策略
    private String riskFactors;            // 风险因素
}
```

## API 端点

### 竞争对手管理

#### 获取所有竞争对手
```
GET /api/ai/competition/competitors
权限: ADMIN, MANAGER, STAFF
```

#### 创建竞争对手
```
POST /api/ai/competition/competitors
权限: ADMIN, MANAGER

请求体:
{
  "name": "竞企A",
  "industry": "建筑业",
  "strengths": "资质齐全，技术实力强",
  "weaknesses": "报价偏高",
  "marketShare": 25.5,
  "typicalBidRangeMin": 1000000,
  "typicalBidRangeMax": 1500000
}
```

### 竞争分析

#### 获取项目竞争分析
```
GET /api/ai/competition/project/{projectId}
权限: ADMIN, MANAGER, STAFF
```

#### 分析项目竞争情况
```
POST /api/ai/competition/project/{projectId}/analyze
权限: ADMIN, MANAGER

返回: 自动生成的竞争分析结果
```

#### 创建竞争分析
```
POST /api/ai/competition/analysis
权限: ADMIN, MANAGER

请求体:
{
  "projectId": 100,
  "competitorId": 1,
  "winProbability": 70.0,
  "competitiveAdvantage": "技术领先",
  "recommendedStrategy": "强调创新",
  "riskFactors": "价格竞争"
}
```

#### 获取竞争对手历史表现
```
GET /api/ai/competition/competitor/{id}/history
权限: ADMIN, MANAGER, STAFF

返回: 该竞争对手的所有历史分析记录（按日期倒序）
```

## 服务方法

### CompetitionIntelService

```java
// 创建竞争对手
CompetitorDTO createCompetitor(CompetitorCreateRequest request)

// 获取所有竞争对手
List<CompetitorDTO> getAllCompetitors()

// 创建竞争分析
CompetitionAnalysisDTO createAnalysis(AnalysisCreateRequest request)

// 根据项目ID获取竞争分析
List<CompetitionAnalysisDTO> getAnalysisByProject(Long projectId)

// 获取竞争对手历史表现
List<CompetitionAnalysisDTO> getHistoricalPerformance(Long competitorId)

// 分析项目竞争情况（自动生成分析）
CompetitionAnalysisDTO analyzeCompetition(Long projectId)
```

## 数据验证规则

### CompetitorCreateRequest

- `name`: 必填，最大200字符
- `industry`: 可选，最大100字符
- `strengths`: 可选，最大5000字符
- `weaknesses`: 可选，最大5000字符
- `marketShare`: 可选，范围0-100
- `typicalBidRangeMin`: 可选，必须>=0
- `typicalBidRangeMax`: 可选，必须>=0且>=typicalBidRangeMin

### AnalysisCreateRequest

- `projectId`: 必填
- `competitorId`: 可选
- `winProbability`: 可选，范围0-100
- `competitiveAdvantage`: 可选，最大5000字符
- `recommendedStrategy`: 可选，最大5000字符
- `riskFactors`: 可选，最大5000字符

## 测试覆盖

模块包含以下测试：

### 单元测试
- `CompetitorTest`: 竞争对手实体测试
- `CompetitionAnalysisTest`: 竞争分析实体测试
- `CompetitionIntelServiceTest`: 服务层业务逻辑测试

### 集成测试
- `CompetitionIntelControllerIntegrationTest`: 控制器集成测试

测试覆盖场景：
- 正常业务流程
- 输入验证
- 边界条件
- 错误处理
- 权限控制

## 数据库索引

### competitors 表
- `idx_competitor_name`: 名称索引
- `idx_competitor_industry`: 行业索引

### competition_analyses 表
- `idx_analysis_project`: 项目ID索引
- `idx_analysis_competitor`: 竞争对手ID索引
- `idx_analysis_date`: 分析日期索引
- `idx_analysis_project_competitor`: 项目+竞争对手复合索引

## 使用示例

### 创建竞争对手并分析

```java
// 1. 创建竞争对手
CompetitorCreateRequest competitorRequest = CompetitorCreateRequest.builder()
    .name("竞企A")
    .industry("建筑业")
    .marketShare(new BigDecimal("25.5"))
    .typicalBidRangeMin(new BigDecimal("1000000"))
    .typicalBidRangeMax(new BigDecimal("1500000"))
    .build();

CompetitorDTO competitor = competitionIntelService.createCompetitor(competitorRequest);

// 2. 为项目创建竞争分析
AnalysisCreateRequest analysisRequest = AnalysisCreateRequest.builder()
    .projectId(100L)
    .competitorId(competitor.getId())
    .winProbability(new BigDecimal("65.0"))
    .competitiveAdvantage("资质齐全，技术实力强")
    .recommendedStrategy("突出技术优势")
    .riskFactors("对手可能采取低价策略")
    .build();

CompetitionAnalysisDTO analysis = competitionIntelService.createAnalysis(analysisRequest);

// 3. 查看历史表现
List<CompetitionAnalysisDTO> history = competitionIntelService.getHistoricalPerformance(competitor.getId());
```

## 审计日志

以下操作会自动记录审计日志：

- 创建竞争对手
- 创建竞争分析
- 分析项目竞争情况

审计日志包含操作类型、实体类型、操作描述等信息。

## 未来扩展

- 集成AI分析能力，自动评估竞争态势
- 数据可视化：竞争对手对比图表
- 胜率预测模型优化
- 市场份额趋势分析
- 竞争对手动态跟踪
