# Collaboration Module (协作记录模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

为投标项目提供团队协作功能，支持项目讨论和评论。

## 概述

本模块实现了项目讨论和评论功能，支持团队成员在投标过程中进行实时协作交流。

## 功能特性

### 讨论线程 (Collaboration Threads)
- 创建项目相关的讨论主题
- 四种状态管理：开放、进行中、已解决、已关闭
- 按项目ID检索所有讨论

### 评论 (Comments)
- 在讨论线程中添加评论
- 支持嵌套回复（父评论-子评论）
- 用户提及功能（@user）
- 软删除机制

### API接口

#### 讨论线程管理
```
GET    /api/collaboration/threads?projectId={projectId}  - 获取项目的讨论列表
GET    /api/collaboration/threads/{id}                    - 获取指定讨论
POST   /api/collaboration/threads                         - 创建新讨论
PUT    /api/collaboration/threads/{id}/status             - 更新讨论状态
```

#### 评论管理
```
POST   /api/collaboration/threads/{id}/comments           - 添加评论
PUT    /api/collaboration/comments/{id}                   - 更新评论
DELETE /api/collaboration/comments/{id}                   - 删除评论（软删除）
```

#### 提及查询
```
GET    /api/collaboration/mentions?userId={userId}        - 获取用户的提及
```

## 权限控制

| 操作 | ADMIN | MANAGER | STAFF |
|------|-------|---------|-------|
| 创建讨论 | ✓ | ✓ | ✗ |
| 查看讨论 | ✓ | ✓ | ✓ |
| 更新状态 | ✓ | ✓ | ✗ |
| 添加评论 | ✓ | ✓ | ✓ |
| 更新评论 | ✓ | ✓ | ✓ |
| 删除评论 | ✓ | ✓ | ✓ |
| 查看提及 | ✓ | ✓ | ✓ |

## 数据模型

### CollaborationThread
```java
{
  "id": 1,
  "projectId": 100,
  "title": "投标策略讨论",
  "status": "OPEN",           // OPEN, IN_PROGRESS, RESOLVED, CLOSED
  "createdBy": 10,
  "createdAt": "2026-03-04T10:00:00",
  "updatedAt": "2026-03-04T10:00:00"
}
```

### Comment
```java
{
  "id": 1,
  "threadId": 1,
  "userId": 10,
  "content": "我认为我们应该采用...",
  "mentions": "[11, 12]",      // JSON格式的用户ID数组
  "parentId": null,            // null表示顶级评论
  "createdAt": "2026-03-04T10:00:00",
  "updatedAt": "2026-03-04T10:00:00",
  "isDeleted": false
}
```

## 使用示例

### 1. 创建讨论线程
```java
ThreadCreateRequest request = ThreadCreateRequest.builder()
    .projectId(100L)
    .title("关于投标保证金的讨论")
    .createdBy(10L)
    .build();

CollaborationThreadDTO thread = collaborationService.createThread(request);
```

### 2. 添加评论
```java
CommentCreateRequest request = CommentCreateRequest.builder()
    .threadId(1L)
    .userId(10L)
    .content("建议准备5万元保证金")
    .mentions("[11, 12]")  // 提及其他用户
    .build();

CommentDTO comment = collaborationService.addComment(1L, request);
```

### 3. 回复评论（嵌套）
```java
CommentCreateRequest request = CommentCreateRequest.builder()
    .threadId(1L)
    .userId(11L)
    .content("同意，我会准备相关材料")
    .parentId(1L)  // 父评论ID
    .build();

CommentDTO reply = collaborationService.addComment(1L, request);
```

### 4. 更新讨论状态
```java
// 将讨论从开放改为进行中
CollaborationThreadDTO updated = collaborationService.updateThreadStatus(
    1L,
    CollaborationThread.ThreadStatus.IN_PROGRESS
);

// 将讨论标记为已解决
CollaborationThreadDTO resolved = collaborationService.updateThreadStatus(
    1L,
    CollaborationThread.ThreadStatus.RESOLVED
);
```

### 5. 软删除评论
```java
collaborationService.deleteComment(commentId);
// 评论不会从数据库删除，仅标记isDeleted=true
```

### 6. 查询用户提及
```java
// 获取所有提及该用户的评论
List<CommentDTO> mentions = collaborationService.getMentionsForUser(userId);
```

## 安全特性

### XSS防护
所有用户输入（标题、内容）都经过`InputSanitizer.stripHtml()`处理，移除危险HTML标签。

### SQL注入防护
使用JPA参数化查询，防止SQL注入攻击。

### 审计日志
所有写操作都通过`@Auditable`注解记录审计日志。

### 软删除
评论删除采用软删除机制，保留数据用于审计和恢复。

## 测试覆盖

### 单元测试
- ✅ Comment实体测试
- ✅ CollaborationThread实体测试
- ✅ CollaborationService业务逻辑测试

### 集成测试
- ✅ CollaborationController API端点测试

### 测试场景
- ✅ 正常业务流程
- ✅ 边界条件处理
- ✅ 异常情况处理
- ✅ 权限验证
- ✅ 输入验证

## 数据库索引优化

```sql
-- 讨论线程表索引
CREATE INDEX idx_thread_project ON collaboration_threads(project_id);
CREATE INDEX idx_thread_status ON collaboration_threads(status);
CREATE INDEX idx_thread_project_status ON collaboration_threads(project_id, status);

-- 评论表索引
CREATE INDEX idx_comment_thread ON comments(thread_id);
CREATE INDEX idx_comment_user ON comments(user_id);
CREATE INDEX idx_comment_parent ON comments(parent_id);
CREATE INDEX idx_comment_deleted ON comments(is_deleted);
CREATE INDEX idx_comment_thread_deleted ON comments(thread_id, is_deleted);
```

## 性能考虑

1. **分页支持**：评论列表可扩展分页功能
2. **索引优化**：为常用查询字段添加索引
3. **软删除**：避免数据丢失，支持审计
4. **批量查询**：支持按项目、线程批量获取数据

## 未来扩展

- [ ] 实时评论推送（WebSocket）
- [ ] 评论点赞/反应功能
- [ ] 文件附件支持
- [ ] 评论搜索功能
- [ ] 标签和分类
- [ ] 通知系统集成
- [ ] 评论编辑历史记录
- [ ] 高级提及功能（@角色、@部门）

## 相关模块

- **项目模块**：讨论与项目关联
- **用户模块**：用户信息和权限
- **审计模块**：操作日志记录

## 技术栈

- Spring Data JPA
- Spring Security
- Lombok
- Jakarta Persistence API
- MySQL 8.0+

## 作者

Implementation Date: 2026-03-04
Follows TDD methodology with 80%+ test coverage.
