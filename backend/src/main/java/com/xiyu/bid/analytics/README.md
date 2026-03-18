# Analytics 模块（合规检查能力）

> 一旦我所属的文件夹有所变化，请更新我。

## 职责说明
合规检查模块负责项目和标讯的合规性校验、风险评估与结果查询。
该目录承载的是规则检查能力，不负责业务主流程编排。
对外暴露统一的检查、查询和风险评估接口。

## 边界清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `entity/` | 子目录 | 合规规则与检查结果实体边界 |
| `entity/ComplianceRule.java` | Entity | 合规规则实体 |
| `entity/ComplianceCheckResult.java` | Entity | 合规检查结果实体 |
| `repository/` | 子目录 | 合规数据访问边界 |
| `repository/ComplianceRuleRepository.java` | Repository | 合规规则数据访问 |
| `repository/ComplianceCheckResultRepository.java` | Repository | 检查结果数据访问 |
| `service/` | 子目录 | 合规检查服务边界 |
| `service/ComplianceCheckService.java` | Service | 合规检查业务逻辑 |
| `controller/` | 子目录 | 合规检查 API 边界 |
| `controller/ComplianceController.java` | Controller | 合规检查 REST API |
| `dto/` | 子目录 | 合规结果与风险评估边界 |
| `dto/ComplianceCheckResultDTO.java` | DTO | 检查结果传输对象 |
| `dto/ComplianceIssue.java` | DTO | 合规问题项 |
| `dto/RiskAssessmentDTO.java` | DTO | 风险评估结果 |
