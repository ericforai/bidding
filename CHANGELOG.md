# 变更日志 (CHANGELOG)

记录项目的重要变更和版本历史。

## [未发布]

## [0.0.2.0] - 2026-04-20

#### 重构 (Refactor)
- 拆分告警规则执行与跨模块调度边界，新增 `alertdispatch` 协调层以消除 `alerts -> businessqualification/resources -> alerts` 架构循环
- 收敛项目详情页启动链路，改为先加载真实项目详情，再分阶段加载任务、文档与对话框依赖，降低页面对渲染时序的偶然依赖
- 恢复 Java 质量门禁 profile，并统一 `pom.xml`、CI workflow 与 README 的执行口径

#### 修复 (Fixes)
- 修复 Flyway 重复迁移版本冲突，顺延保证金跟踪、历史项目快照与案例资产扩展脚本版本号
- 修复真实 API 认证链路：`/login` 遇到 stale user hint 时自动尝试恢复会话，`/api/auth/logout` 保留 `Authorization` 注入但禁止 refresh 重试
- 修复项目详情、商务详情弹窗与文档编辑归档摘要链路，确保真实 API 模式下页面与摘要字段行为稳定

### 2026-04-20

#### 新增 (Added)
- 资源费用模块新增“支付登记 + 支付流水查询”闭环：
  - 后端新增费用支付记录实体、DTO、仓储与 `/api/resources/expenses/{id}/payments` 接口
  - 费用详情、列表和项目费用查询返回最近一次支付摘要，支持前端直接展示当前支付状态
  - 费用页支持支付登记、支付历史查看、保证金退还申请与确认
- 项目详情页接入真实项目费用归集，展示总额、已支付、待支付、已退还等汇总指标

#### 重构 (Refactor)
- 后端费用能力按门面、命令、查询、支付三层拆分，避免单个服务同时承担规则、查询和写入职责
- 前端费用页与项目详情页按页面壳、业务组件、对话框、组合式函数拆分，收敛超大单文件

#### 修复 (Fixes)
- 修复支付跟踪合并 `dev` 后的提醒接口兼容问题，补齐 `sendReturnReminder` API 兼容入口
- 修复费用申请未透传 `expectedReturnDate` 导致保证金应退日期丢失的问题
- 修复费用页回归测试，改为匹配当前拆分后的组件结构

#### 新增 (Added)
- 资源费用模块新增“保证金退还自动跟踪提醒”闭环：
  - 费用申请支持维护 `expectedReturnDate`
  - 根据已确认开标结果 + 预计退还日期自动扫描未退还保证金
  - 生成真实告警历史并记录 `lastReturnReminderAt`
  - 费用页展示真实应退日期、跟踪状态、最近提醒时间
  - 支持手工发送保证金退还提醒与确认退还

#### 重构 (Refactor)
- 按 Split-First Rule 拆分 `Expense.vue`，提取 `useExpensePage.js`、保证金跟踪逻辑和多个页面子组件
- 拆分告警调度执行边界，引入 `AlertRuleExecutionService` 承接保证金退还扫描

#### 修复 (Fixes)
- 修复资源费用页“费用申请”按钮无法打开申请弹窗的问题，并补充回归测试
- 修复手工保证金提醒在系统配置缺失时可能出现的空指针，并补充默认阈值兜底测试
- 修复告警规则前端对真实后端字段的映射，补齐 `QUALIFICATION_EXPIRY` / `DEPOSIT_RETURN` 类型展示与编辑能力

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
