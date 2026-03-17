# Service - 业务逻辑层

一旦我所属的文件夹有所变化，请更新我。

## 功能概述
实现核心业务逻辑，处理事务管理，调用Repository进行数据访问，提供可复用的业务功能。

## 文件清单
- `AuthService.java` - 认证服务，用户注册/登录/令牌管理
- `TenderService.java` - 标讯服务，AI分析计算
- `ProjectService.java` - 项目服务，状态流转管理
- `TaskService.java` - 任务服务，分配与进度跟踪
- `QualificationService.java` - 资质服务
- `CaseService.java` - 案例服务
- `TemplateService.java` - 模板服务
- `KnowledgeService.java` - 知识库服务（已拆分为3个独立服务）
- `AuditLogService.java` - 审计日志服务
