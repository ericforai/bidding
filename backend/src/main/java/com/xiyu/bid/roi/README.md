# ROI分析模块 (ROI Analysis Module)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

提供投资回报率(ROI)计算和分析功能，帮助项目团队评估投标项目的盈利能力和风险。

## 功能特性

- **ROI计算**: 自动计算投资回报率
- **敏感性分析**: 分析不同成本和收入变化对ROI的影响
- **风险因素记录**: 记录和跟踪项目风险
- **假设条件管理**: 管理ROI计算的基础假设
- **审计日志**: 支持操作审计追踪

## 核心概念

### ROI计算公式

```
利润 = 收入 - 成本
ROI% = (利润 / 成本) × 100
```

### 敏感性分析

敏感性分析通过改变成本和收入参数，生成多种场景：
- **最佳场景**: 成本降低，收入增加
- **最差场景**: 成本增加，收入减少
- **基础场景**: 当前预估值

## API端点

### 1. 获取项目ROI分析

```http
GET /api/ai/roi/project/{projectId}
```

**权限**: ADMIN, MANAGER, STAFF

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "Successfully retrieved ROI analysis",
  "data": {
    "id": 1,
    "projectId": 100,
    "analysisDate": "2024-03-01T10:00:00",
    "estimatedCost": 500000.00,
    "estimatedRevenue": 800000.00,
    "estimatedProfit": 300000.00,
    "roiPercentage": 60.00,
    "paybackPeriodMonths": 24,
    "riskFactors": "Market volatility, regulatory changes",
    "assumptions": "Project completion on time, no cost overruns"
  }
}
```

### 2. 创建ROI分析

```http
POST /api/ai/roi
```

**权限**: ADMIN, MANAGER

**请求体**:
```json
{
  "projectId": 100,
  "estimatedCost": 500000.00,
  "estimatedRevenue": 800000.00,
  "paybackPeriodMonths": 24,
  "riskFactors": "Market volatility, regulatory changes",
  "assumptions": "Project completion on time, no cost overruns",
  "createdBy": 1
}
```

**响应**: HTTP 201 Created

### 3. 计算项目ROI

```http
POST /api/ai/roi/project/{projectId}/calculate
```

**权限**: ADMIN, MANAGER

**说明**:
- 如果项目已有ROI分析，则更新
- 如果项目没有ROI分析，则创建

### 4. 执行敏感性分析

```http
POST /api/ai/roi/sensitivity
```

**权限**: ADMIN, MANAGER, STAFF

**请求体**:
```json
{
  "projectId": 100,
  "costVariations": [-10.0, 0.0, 10.0],
  "revenueVariations": [-10.0, 0.0, 10.0]
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "projectId": 100,
    "baseCost": 500000.00,
    "baseRevenue": 800000.00,
    "baseProfit": 300000.00,
    "baseROI": 60.00,
    "scenarios": [
      {
        "costVariation": -10.0,
        "revenueVariation": 10.0,
        "adjustedCost": 450000.00,
        "adjustedRevenue": 880000.00,
        "adjustedProfit": 430000.00,
        "adjustedROI": 95.56,
        "description": "Best case"
      },
      {
        "costVariation": 0.0,
        "revenueVariation": 0.0,
        "adjustedCost": 500000.00,
        "adjustedRevenue": 800000.00,
        "adjustedProfit": 300000.00,
        "adjustedROI": 60.00,
        "description": "Base case"
      },
      {
        "costVariation": 10.0,
        "revenueVariation": -10.0,
        "adjustedCost": 550000.00,
        "adjustedRevenue": 720000.00,
        "adjustedProfit": 170000.00,
        "adjustedROI": 30.91,
        "description": "Worst case"
      }
    ]
  }
}
```

## 数据模型

### ROIAnalysis Entity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| projectId | Long | 项目ID（必填） |
| analysisDate | LocalDateTime | 分析日期 |
| estimatedCost | BigDecimal(19,2) | 预估成本 |
| estimatedRevenue | BigDecimal(19,2) | 预估收入 |
| estimatedProfit | BigDecimal(19,2) | 预估利润（自动计算） |
| roiPercentage | BigDecimal(10,2) | ROI百分比（自动计算） |
| paybackPeriodMonths | Integer | 回收期（月） |
| riskFactors | TEXT | 风险因素 |
| assumptions | TEXT | 假设条件 |
| createdBy | Long | 创建人ID |

## 使用示例

### Java客户端示例

```java
// 1. 创建ROI分析
ROIAnalysisCreateRequest request = ROIAnalysisCreateRequest.builder()
    .projectId(100L)
    .estimatedCost(new BigDecimal("500000.00"))
    .estimatedRevenue(new BigDecimal("800000.00"))
    .paybackPeriodMonths(24)
    .riskFactors("Market volatility, regulatory changes")
    .assumptions("Project completion on time, no cost overruns")
    .createdBy(1L)
    .build();

ROIAnalysisDTO analysis = roiAnalysisService.createAnalysis(request);
System.out.println("ROI: " + analysis.getRoiPercentage() + "%");

// 2. 执行敏感性分析
SensitivityAnalysisRequest sensitivityRequest = SensitivityAnalysisRequest.builder()
    .projectId(100L)
    .costVariations(Arrays.asList(-10.0, 0.0, 10.0))
    .revenueVariations(Arrays.asList(-10.0, 0.0, 10.0))
    .build();

SensitivityAnalysisResult result = roiAnalysisService.performSensitivityAnalysis(100L, sensitivityRequest);
result.getScenarios().forEach(scenario ->
    System.out.println(scenario.getDescription() + ": " + scenario.getAdjustedROI() + "%")
);
```

### cURL示例

```bash
# 创建ROI分析
curl -X POST http://localhost:8080/api/ai/roi \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "projectId": 100,
    "estimatedCost": 500000.00,
    "estimatedRevenue": 800000.00,
    "paybackPeriodMonths": 24,
    "createdBy": 1
  }'

# 获取ROI分析
curl -X GET http://localhost:8080/api/ai/roi/project/100 \
  -H "Authorization: Bearer {token}"

# 执行敏感性分析
curl -X POST http://localhost:8080/api/ai/roi/sensitivity \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "projectId": 100,
    "costVariations": [-10.0, 0.0, 10.0],
    "revenueVariations": [-10.0, 0.0, 10.0]
  }'
```

## 测试

### 单元测试

```bash
# 运行所有ROI测试
mvn test -Dtest=ROIAnalysis*Test

# 运行Service测试
mvn test -Dtest=ROIAnalysisServiceTest

# 运行Repository测试
mvn test -Dtest=ROIAnalysisRepositoryTest

# 运行Controller测试
mvn test -Dtest=ROIAnalysisControllerTest
```

### 测试覆盖率

```bash
# 生成覆盖率报告
mvn jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

**当前覆盖率**: 80%+ (Service层)

## 架构设计

### 分层架构

```
Controller Layer (ROIAnalysisController)
    ↓
Service Layer (ROIAnalysisService)
    ↓
Repository Layer (ROIAnalysisRepository)
    ↓
Database (PostgreSQL/H2)
```

### 关键设计模式

1. **Repository Pattern**: 数据访问抽象
2. **DTO Pattern**: 数据传输对象
3. **Builder Pattern**: 对象构建
4. **Strategy Pattern**: 敏感性分析场景生成

### 依赖注入

- 使用Spring依赖注入
- IAuditLogService接口注入，便于测试Mock
- @Auditable注解实现审计日志

## 安全性

### 输入验证

- 所有输入使用Jakarta Validation注解
- 使用InputSanitizer清洗用户输入
- SQL注入防护（JPA参数化查询）
- XSS防护（输入清洗）

### 权限控制

| 端点 | ADMIN | MANAGER | STAFF |
|------|-------|---------|-------|
| GET /api/ai/roi/project/{projectId} | ✓ | ✓ | ✓ |
| POST /api/ai/roi | ✓ | ✓ | ✗ |
| POST /api/ai/roi/project/{projectId}/calculate | ✓ | ✓ | ✗ |
| POST /api/ai/roi/sensitivity | ✓ | ✓ | ✓ |

## 性能考虑

1. **数据库索引**:
   - idx_roi_project: 项目ID索引
   - idx_roi_analysis_date: 分析日期索引

2. **查询优化**:
   - 使用findByProjectId直接查询
   - 避免N+1查询问题

3. **大数据处理**:
   - 敏感性分析最多支持100个场景
   - TEXT字段支持大文本存储

## 错误处理

| 异常类型 | HTTP状态码 | 说明 |
|----------|-----------|------|
| IllegalArgumentException | 400 | 输入参数无效 |
| ResourceNotFoundException | 404 | ROI分析不存在 |
| IllegalStateException | 409 | 状态冲突 |

## 扩展性

### 未来增强

1. **多币种支持**: 支持不同货币的ROI计算
2. **历史趋势**: 追踪ROI变化历史
3. **对比分析**: 多项目ROI对比
4. **导出功能**: 导出ROI报告为PDF/Excel
5. **图表可视化**: 生成ROI趋势图表

### 集成点

- Project模块: 关联项目信息
- AuditLog模块: 审计日志记录
- Analytics模块: 数据分析和报表

## 文件结构

```
src/main/java/com/xiyu/bid/roi/
├── entity/
│   └── ROIAnalysis.java              # ROI分析实体
├── repository/
│   └── ROIAnalysisRepository.java    # 数据访问层
├── service/
│   └── ROIAnalysisService.java       # 业务逻辑层
├── controller/
│   └── ROIAnalysisController.java    # 控制器层
└── dto/
    ├── ROIAnalysisDTO.java           # ROI分析DTO
    ├── ROIAnalysisCreateRequest.java # 创建请求DTO
    ├── SensitivityAnalysisRequest.java # 敏感性分析请求
    └── SensitivityAnalysisResult.java   # 敏感性分析结果

src/test/java/com/xiyu/bid/roi/
├── ROIAnalysisServiceTest.java       # Service测试
├── ROIAnalysisRepositoryTest.java    # Repository测试
└── ROIAnalysisControllerTest.java    # Controller测试
```

## 贡献指南

1. 遵循TDD开发流程
2. 保持80%+测试覆盖率
3. 添加适当的JavaDoc注释
4. 遵循项目编码规范
5. 运行所有测试后再提交

## 许可证

Copyright © 2024 XiYu Bidding System
