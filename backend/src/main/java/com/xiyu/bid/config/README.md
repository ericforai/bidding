# Config - 配置类

一旦我所属的文件夹有所变化，请更新我。

## 功能概述
系统核心配置，包括安全配置、JWT配置、CORS配置、异步任务配置、速率限制配置等。

## 文件清单
- `SecurityConfig.java` - Spring Security配置，角色权限控制
- `JwtConfig.java` - JWT Bean配置
- `AsyncConfig.java` - 异步任务线程池配置
- `RateLimitConfig.java` - 速率限制配置
- `RateLimitFilter.java` - 登录速率限制过滤器
