# Auth Module (认证授权模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

提供基于 JWT 的用户认证和授权功能，保护 API 端点安全。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `JwtUtil.java` | Util | JWT 令牌生成和验证工具 |
| `JwtAuthenticationFilter.java` | Filter | JWT 认证过滤器，拦截请求验证令牌 |
| `UserDetailsServiceImpl.java` | Service | Spring Security 用户详情服务实现 |

## 核心功能

### JWT 认证流程
1. 用户登录 → 生成 JWT 令牌
2. 后续请求携带令牌 → Filter 拦截验证
3. 验证通过 → 放行到业务逻辑
4. 验证失败 → 返回 401 未授权

### 令牌管理
- **生成**: 登录成功后生成，包含用户信息
- **验证**: 每次请求时验证令牌有效性
- **过期**: 默认 24 小时有效期

## 依赖

- Spring Security
- JJWT (io.jsonwebtoken:jjwt-api)
- 用户模块 (User, UserRepository)

## 相关模块

- `controller/AuthController.java` - 登录/注册端点
- `entity/User.java` - 用户实体
