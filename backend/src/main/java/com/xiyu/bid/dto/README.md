# DTO Package (数据传输对象包)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

存放所有模块共享的数据传输对象 (DTO)，包括 API 请求/响应结构。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `ApiResponse.java` | DTO | 统一 API 响应包装类 |
| `LoginRequest.java` | DTO | 登录请求 |
| `RegisterRequest.java` | DTO | 注册请求 |
| `AuthResponse.java` | DTO | 认证响应 |
| `TenderDTO.java` | DTO | 标讯数据传输对象 |
| `ProjectDTO.java` | DTO | 项目数据传输对象 |
| `TaskDTO.java` | DTO | 任务数据传输对象 |
| `QualificationDTO.java` | DTO | 资质数据传输对象 |
| `CaseDTO.java` | DTO | 案例数据传输对象 |
| `TemplateDTO.java` | DTO | 模板数据传输对象 |

## 设计模式

- **Request DTO**: 封装 API 请求参数
- **Response DTO**: 封装 API 响应数据
- **统一响应**: 使用 `ApiResponse<T>` 包装所有响应

## 统一响应格式

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```
