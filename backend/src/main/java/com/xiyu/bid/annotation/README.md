# Annotation Package (注解包)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

定义自定义注解，用于 AOP 切面编程和功能增强。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `Auditable.java` | Annotation | 审计日志注解 |

## @Auditable 注解

标记需要记录审计日志的方法。

```java
@Auditable(action = "CREATE", entityType = "Project")
public ProjectDTO createProject(ProjectCreateRequest request) {
    // ...
}
```

## AOP 处理

由 `AuditAspect` 拦截带 `@Auditable` 注解的方法，自动记录：
- 操作类型 (CREATE, UPDATE, DELETE)
- 实体类型
- 操作描述
- 操作人
- 操作时间
