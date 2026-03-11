# Aspect Package (切面包)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

存放 AOP 切面类，实现横切关注点（审计日志、权限检查等）。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `AuditAspect.java` | Aspect | 审计日志切面 |

## 切面职责

### AuditAspect
- 拦截 `@Auditable` 注解的方法
- 自动记录操作日志到 `audit_logs` 表
- 提取方法参数和返回值

## 切入点配置

```java
@Around("@annotation(auditable)")
public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) {
    // 1. 记录开始时间
    // 2. 执行目标方法
    // 3. 记录审计日志
    // 4. 返回结果
}
```
