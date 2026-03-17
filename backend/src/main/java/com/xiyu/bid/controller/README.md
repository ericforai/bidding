# Controller - REST API控制器层

一旦我所属的文件夹有所变化，请更新我。

## 功能概述
处理HTTP请求，提供RESTful API端点，负责参数验证、权限控制，调用Service层处理业务逻辑，返回标准化响应。

## 文件清单
- `AuthController.java` - 认证控制器，登录/注册/令牌刷新
- `TenderController.java` - 标讯管理控制器，CRUD + AI分析
- `ProjectController.java` - 项目管理控制器，状态流转 + 团队管理
- `TaskController.java` - 任务管理控制器，分配 + 进度跟踪
- `QualificationController.java` - 资质管理控制器
- `CaseController.java` - 案例管理控制器
- `TemplateController.java` - 模板管理控制器
- `TestController.java` - 测试控制器
