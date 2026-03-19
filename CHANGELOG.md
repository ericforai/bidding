# 变更日志 (CHANGELOG)

记录项目的重要变更和版本历史。

## [未发布]

### 2026-03-19

#### 安全修复 (Security Fixes)
- **PasswordEncryptionUtil.java**: 修复硬编码加密密钥的 P0 安全漏洞
  - 使用 `PLATFORM_ENCRYPTION_KEY` 环境变量替代硬编码密钥
  - 添加开发/测试环境的 fallback 机制
  - 添加生产环境启动验证（非开发环境必须有环境变量）
  - **TDD 实现**: 20 个单元测试 + 3 个集成测试，覆盖率 > 80%

#### 修复 (Fixes)
- **client.js**: 修复导航跳转绕过 Vue Router 守卫的问题
  - 使用 `router.push()` 替代 `window.location.href`
  - 确保路由守卫正确触发
  - 添加 NavigationDuplicated 错误处理
  - 添加 E2E 测试覆盖路由跳转场景

#### 文档 (Docs)
- 更新 `TECHNICAL_DEBT.md`: 记录 P0 修复完成状态
- 添加密码加密安全修复的 TDD 实施报告
- 添加路由导航修复摘要

---

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
