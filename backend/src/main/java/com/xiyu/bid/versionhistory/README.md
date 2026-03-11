# Version History Module (版本历史模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

跟踪投标文档的版本变更历史，支持版本对比(diff)、版本回滚和变更记录查询。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `DocumentVersion.java` | Entity | 文档版本实体 |
| `DocumentVersionRepository.java` | Repository | 版本历史数据访问层 |
| `VersionHistoryService.java` | Service | 版本历史业务逻辑层 |
| `DocumentVersionController.java` | Controller | REST API 端点 |
| `DocumentVersionDTO.java` | DTO | 版本数据传输对象 |
| `VersionDiffDTO.java` | DTO | 版本对比结果 |
| `VersionCreateRequest.java` | DTO | 创建版本请求 |

## API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/documents/{id}/versions` | 获取版本列表 |
| POST | `/api/documents/{id}/versions` | 创建新版本 |
| GET | `/api/documents/{id}/versions/{versionId}` | 获取版本详情 |
| GET | `/api/documents/{id}/versions/diff` | 版本对比 |
| POST | `/api/documents/{id}/versions/rollback` | 版本回滚 |
| GET | `/api/documents/{id}/versions/current` | 获取当前版本 |

## 特性

- **自动版本**: 文档修改时自动创建版本
- **版本对比**: 支持两个版本之间的差异对比
- **版本回滚**: 可回滚到任意历史版本
- **变更记录**: 记录每次变更的作者、时间和原因

## 字段说明

- `versionNumber`: 版本号 (递增)
- `content`: 文档内容快照
- `changeSummary`: 变更摘要
- `createdBy`: 创建人
- `createdAt`: 创建时间
