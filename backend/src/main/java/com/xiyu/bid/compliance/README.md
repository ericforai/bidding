# Compliance Check Module

> 一旦我所属的文件夹有所变化，请更新我。

## 目录功能

合规检查服务模块，提供项目/标讯的合规性检查、风险评估功能。

## 文件清单

### Entity (实体层)
- `entity/ComplianceRule.java` - 合规规则实体
- `entity/ComplianceCheckResult.java` - 合规检查结果实体

### Repository (数据访问层)
- `repository/ComplianceRuleRepository.java` - 合规规则数据访问
- `repository/ComplianceCheckResultRepository.java` - 检查结果数据访问

### Service (业务层)
- `service/ComplianceCheckService.java` - 合规检查业务逻辑

### Controller (控制器层)
- `controller/ComplianceController.java` - 合规检查 REST API 端点

### DTO (数据传输对象)
- `dto/ComplianceCheckResultDTO.java` - 检查结果传输对象
- `dto/ComplianceIssue.java` - 合规问题
- `dto/RiskAssessmentDTO.java` - 风险评估结果

## API 端点

- `POST /api/compliance/check/project/{projectId}` - 检查项目合规性
- `POST /api/compliance/check/tender/{tenderId}` - 检查标讯合规性
- `GET /api/compliance/results/{resultId}` - 获取检查结果
- `GET /api/compliance/project/{projectId}/results` - 项目检查历史
- `GET /api/compliance/assess-risk/{projectId}` - 风险评估

## 规则类型

- QUALIFICATION - 资质要求
- DOCUMENT - 文档完整性
- FINANCIAL - 财务要求
- EXPERIENCE - 经验要求
- DEADLINE - 时间要求

## 检查状态

- COMPLIANT - 合规
- NON_COMPLIANT - 不合规
- PARTIAL_COMPLIANT - 部分合规
- WARNING - 警告
