# Frontend 真实数据源治理约定

本文件定义前端在 `mock` / `api` 双模式下的统一开发规则，用于约束页面、组件、store、API 模块与演示数据之间的边界。

目标只有一个：**`api` 模式下绝不允许 mock 泄漏到真实产品链路。**

## 1. 基本原则

- `mock` 模式只服务演示和方案预览。
- `api` 模式只允许真实后端、真实空态、真实失败态、显式未接入态。
- 页面、组件、store 不得直接读取 `mockData`。
- 页面、组件不得直接读写 `demoPersistence`。
- demo 数据只能通过 API adapter 或 API module 按模式显式注入。

## 2. 分层边界

### 页面 / 组件层

- 禁止 `import '@/api/mock'`
- 禁止 `import '@/utils/demoPersistence'`
- 禁止组件内部定义本地 `mockData` 作为真实模式 fallback
- 只允许消费：
  - store 提供的数据
  - API module 返回的数据
  - mock adapter 提供的最终视图数据

### Store 层

- 只允许调用 API module 或 mock adapter
- 不允许直接拼接 `mockData` 作为兜底
- `api` 模式请求失败时，只能返回空列表、空对象、失败态或上抛错误

### API 模块层

- 是前端业务数据的唯一入口
- 可以根据 `isMockMode()` 分流到：
  - 真实 HTTP
  - `src/api/mock-adapters/**`
  - `src/api/mock.js`
- 对于后端未实现能力，必须返回：
  - `FEATURE_UNAVAILABLE`
  - 空态
  - 明确失败
- 禁止伪造“成功但其实是演示结果”的 API 响应

### Mock Adapter 层

- 统一放在 `src/api/mock-adapters/**`
- 负责：
  - 读取 `mockData`
  - 读取/写入 `demoPersistence`
  - 将 demo 数据整理成页面可直接消费的最终结构
- 页面和组件不得绕过 adapter 直接访问 demo 原始数据

## 3. 模块治理分类

### A 类：必须真实化

以下模块在 `api` 模式下必须做到零 mock fallback：

- Project
- Resource
- Collaboration
- Dashboard

要求：

- 不得回退演示数据
- 不得伪造成功态
- 只允许真实后端、真实空态、真实失败态

### B 类：暂未接入但保留入口

例如：

- Customer Opportunity Center
- 部分 AI 衍生能力

要求：

- 统一展示未接入 / 未开放
- 不得模拟扫描成功、分析成功、转项目成功

### C 类：纯演示资产

- 保留在 `src/api/mock.js`
- 仅允许被 API module 或 mock adapter 使用
- 不得被业务页面直接消费

## 4. 代码约束

### 允许

- `src/api/modules/** -> src/api/mock.js`
- `src/api/modules/** -> src/api/mock-adapters/**`
- `src/api/mock-adapters/** -> src/utils/demoPersistence.js`

### 禁止

- `src/views/** -> @/api/mock`
- `src/components/** -> @/api/mock`
- `src/stores/** -> @/api/mock`
- `src/views/** -> @/utils/demoPersistence`
- `src/components/** -> @/utils/demoPersistence`
- `src/stores/** -> @/utils/demoPersistence`
- `@/api` 再导出 `mockData` 给业务层使用

## 5. 开发流程要求

新增或修改前端功能时，必须按下面顺序处理：

1. 先判断该能力属于 A / B / C 哪一类
2. 明确 `api` 模式下的真实行为：
   - 真实数据
   - 空态
   - 失败态
   - 未接入态
3. 如需 demo 数据，先放入 mock adapter，再接入页面
4. 不允许在页面层临时写 `if (isMockMode())` 后直接拼演示对象
5. 所有新页面都必须通过边界扫描再进入提交

## 6. 构建与门禁

当前仓库已接入：

- `npm run check:front-data-boundaries`
- `npm run build` 会先执行边界扫描

扫描会阻断以下问题：

- 页面 / 组件 / store 直接导入 `@/api/mock`
- 页面 / 组件 / store 直接导入 `demoPersistence`
- 业务层从 `@/api` 解构 `mockData`
- 业务层重新定义本地 `mockData` fallback

## 7. 迁移规则

- 老代码从业务层迁移时，优先把 demo 读取逻辑搬到 mock adapter
- 无后端契约的能力，不继续补假数据，直接改为未接入态
- 只有当真实接口和响应结构稳定后，才允许把某模块从 B 类提升到 A 类

## 8. 验收标准

满足以下条件，才算完成某一模块的真实数据源治理：

- `api` 模式下无 mock fallback
- 页面、组件、store 无直连 `mock.js`
- 页面、组件无直连 `demoPersistence`
- 构建扫描通过
- 页面在真实模式下只表现为：
  - 真实数据
  - 真实空态
  - 真实失败态
  - 明确未接入态

## 9. 相关文件

- `src/api/mock.js`
- `src/api/mock-adapters/frontendDemo.js`
- `src/utils/demoPersistence.js`
- `scripts/check-front-data-boundaries.mjs`
- `docs/MOCK_MIGRATION_BACKLOG.md`

