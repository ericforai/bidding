# 变更日志 (CHANGELOG)

记录项目的重要变更和版本历史。

## [未发布]

### 2026-04-20

#### 新增 (Features)
- **标准模板库三维分类闭环**: 以真实 `/api/knowledge/templates` API 为入口，补齐按产品类型、行业、文档类型分类的最小客户验收闭环
  - 后端新增 `templatecatalog` 增量模块，落实模板三维分类、受控字典校验、版本规则与组合筛选
  - 数据模型补齐 `productType`、`industry`、`documentType` 字段，并保持历史 `category` 兼容
  - 前端模板库页面完成拆分，新增三维筛选、受控下拉表单与真实 API 联动
  - 新增真实 API 场景下的后端集成测试与 E2E 覆盖

#### 重构 (Refactor)
- **FP-Java / Split-First 收口**: 收敛模板域、资质库与相关页面职责边界
  - 模板域核心规则下沉到纯核心，应用服务只负责用例编排
  - 拆分超大前端页面与 API 模块，减少职责混杂与单文件膨胀
  - 强化架构门禁，补齐 Architecture / FP-Java 相关测试约束
- **全绿稳定化收口**: 收敛告警调度、项目详情启动链路与 Java 质量门禁
  - 新增 `alertdispatch` 协调层，解除 `alerts -> businessqualification/resources -> alerts` 架构循环
  - 收敛项目详情页与费用页的页面壳/组合式函数拆分，降低页面对渲染时序的偶然依赖
  - 恢复 `quality-strict`、Flyway 容器校验与 CI 门禁的一致口径

#### 修复 (Fixes)
- **Ship 收口兼容修复**: 修复工作台与标讯页在现有单元测试约定下的兼容问题
  - 恢复 `alerts` / `fees` API 兼容方法，消除历史调用断裂
  - 为工作台补齐数据归一化工具，稳定真实 API 数据映射
  - 调整 `Bidding/List` 视图与测试桩，消除 slot props 缺失导致的测试失败
- **版本治理补齐**: 新增仓库根目录 `VERSION`，并将前后端版本号收敛到同一来源
  - 新增版本同步与一致性检查脚本
  - 构建与发布预检接入版本一致性门禁
- **稳定化修复**: 补齐认证恢复、项目详情、文档归档摘要与资源费用闭环
  - 修复 Flyway 重复迁移版本冲突，并顺延保证金跟踪、历史项目快照与案例资产扩展脚本版本号
  - 修复 `/login` stale user hint 会话恢复与 `/api/auth/logout` 的 `Authorization`/refresh 策略
  - 补齐费用支付登记、支付流水查询、保证金退还自动跟踪与手工提醒链路
  - 修复告警规则、费用页和项目详情相关真实 API 回归问题

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
