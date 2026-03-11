# Repository Package (数据访问层包)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

存放 JPA Repository 接口，提供数据库访问能力。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `UserRepository.java` | Repository | 用户数据访问 |
| `TenderRepository.java` | Repository | 标讯数据访问 |
| `ProjectRepository.java` | Repository | 项目数据访问 |
| `TaskRepository.java` | Repository | 任务数据访问 |
| `QualificationRepository.java` | Repository | 资质数据访问 |
| `CaseRepository.java` | Repository | 案例数据访问 |
| `TemplateRepository.java` | Repository | 模板数据访问 |
| `AuditLogRepository.java` | Repository | 审计日志数据访问 |

## 设计模式

继承 `JpaRepository<T, ID>` 获得：
- 基本 CRUD 操作
- 分页查询支持
- 排序功能

## 自定义查询

```java
// 方法名查询
List<Project> findByStatus(ProjectStatus status);

// @Query 注解
@Query("SELECT p FROM Project p WHERE p.createdBy = :userId")
List<Project> findByCreatedBy(@Param("userId") Long userId);
```
