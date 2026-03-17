# 变更日志 (CHANGELOG)

记录项目的重要变更和版本历史。

## [未发布]

### 2026-03-11

#### 修复 (Fixes)
- **project.js**: 添加 API 失败时的 mock 数据回退逻辑，解决白屏问题
- **List.vue**: 修复"查看详情"按钮路由跳转失败问题
  - 添加 async/await 错误处理
  - 添加调试日志
  - 实现编辑按钮跳转功能
- **mock.js**: 修复语法错误（删除多余的 `}`）

#### 文档 (Docs)
- 新增 `src/stores/README.md`
- 新增 `src/api/README.md`
- 新增 `src/views/Project/README.md`
- 为修改的源码文件添加标准头部注释

---
