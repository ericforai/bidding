# RULES.md — 项目强制红线与开发作业流程

本文件定义本项目当前有效的开发红线、执行步骤和已知存量例外。
目标是让参与项目的人类与 AI 代理都能区分：什么是已经落地的硬门禁，什么是当前代码仍未收口但必须继续清理的历史问题。

---

## 1. 标准作业流程（SOP）

所有功能开发、Bug 修复、重构任务都必须按以下四阶段执行，不得直接跳到“改代码”。

### Phase 1: Plan（规划）

- 开始实现前先明确目标、改动范围、验收标准和风险点。
- 涉及多个模块、架构边界或测试基线调整的任务，必须先写清楚影响面，再进入实现。
- 禁止“边写边猜”；不清楚接口、状态流转或验收方式时，先查清现状。

### Phase 2: TDD（测试驱动）

- 优先遵循 Red → Green → Refactor 循环：先补/写测试，再补实现。
- 前端优先使用 Vitest；后端优先使用 JUnit；跨链路回归使用 Playwright。
- 最低要求不是“有测试文件”，而是能证明本次变更确实被验证过。
- 当前仓库允许按影响面分层验证，不要求每次都无脑跑全仓所有测试；但必须如实报告哪些跑了、哪些没跑、哪些受现有基线问题影响。

### Phase 3: Code Review（代码审查）

- 实现完成后，必须主动做自审。
- 检查重点：分层边界、依赖方向、安全输入、错误处理、回归风险、文档同步。
- 发现问题先修，不得把“已知问题”伪装成“后续再说”，除非它本来就是存量基线问题且本次未扩大影响。

### Phase 4: Refactor-Clean（重构与清理）

- 在验证通过后再做清理：消重、提炼命名、删死代码、删无效分支。
- 若发现历史双模式、Mock 回退或无用开关，应优先往“删除”方向收口，而不是继续兼容。
- 清理后重新运行相关验证，确认没有引入新的回归。

---

## 2. 核心业务逻辑架构约束

本节约束的是**业务核心逻辑**，目标是防止 AI 代理把数据读取、状态更新、异常处理和业务计算混成难以测试的面条代码。

执行口径：
- 核心业务逻辑必须优先写成可单元测试的纯函数或近似纯函数。
- Controller、Repository、API adapter、Pinia action、Vue 组件事件、事务编排服务属于命令式外壳，允许副作用，但不得承载复杂业务规则。
- 如因框架约束必须在外壳层更新状态，必须先把核心计算提炼到独立函数、Service 或领域方法中。
- 后端新写纯核心代码时，优先放入 `..core..` 或 `..domain..` 包；这些包下的非 `..entity..` 类受 `FPJavaArchitectureTest` 强制约束。

### 2.0 Split-First Rule（防上帝类默认口径）

任何后端功能实现前，必须先按职责拆分，而不是直接堆进一个 `*Service`：

- **Application Service**：只做编排，负责取数、调用纯核心、事务、保存和边界转换。
- **Domain Policy / Rules**：只做业务规则、校验、状态流转、金额/评分/权限计算，默认保持纯函数或近似纯函数。
- **Mapper / Assembler**：只做 DTO / Entity / ViewModel 转换，不承载业务判断。
- **Repository / Gateway**：只做数据库或外部系统访问，不承载业务规则。
- **Decision / Result Object**：复杂业务分支优先返回显式结果对象，不允许靠超长 `Service` 方法藏状态变化。

默认文件预算：
- 单个 Java 文件软上限 `200` 行。
- 单个 Java 文件硬上限 `300` 行。
- 超过硬上限前必须先拆分职责，不得继续追加代码把文件做成上帝类。

### 2.1 纯核心与命令式外壳

业务计算、校验、状态流转、金额计算、评分计算、权限判断必须优先放在纯核心中。

纯核心不得直接调用：
- 数据库、Repository、ORM 查询
- HTTP API、外部系统 SDK
- LocalStorage、SessionStorage、文件系统
- 控制台日志
- 路由跳转
- 当前时间、随机数等隐式输入

允许副作用的位置：
- 前端：Vue 组件事件、Pinia action、API 模块、浏览器适配层
- 后端：Controller、Repository、Gateway、Adapter、事务编排 Service

要求：
- 外壳层负责取数、保存、调用外部系统和状态提交。
- 核心层只负责根据显式输入计算显式输出。
- 新增业务规则时，不得直接塞进 API 调用、组件事件或数据库事务代码中。

### 2.2 业务计算函数默认无副作用

领域计算函数必须满足：
- 输入来自显式参数。
- 输出来自返回值。
- 不修改传入对象或数组。
- 不读写全局状态、Store、组件实例字段或静态可变字段。
- 不隐藏调用 API、数据库、文件、浏览器存储。

禁止把复杂业务规则写成只修改外部状态的 `void`/无返回值方法。

例外：
- Vue/Pinia/Spring 等外壳层方法可以更新状态或返回框架要求的类型。
- 这些方法只能做编排，复杂业务规则应提炼到可测试的函数或服务中。

### 2.3 业务错误作为普通值返回

业务可预期失败必须作为普通值返回，例如：
- 校验失败
- 找不到业务对象
- 状态不允许流转
- 权限不足
- 额度不足
- 数据不完整

推荐返回结构：

```js
{
  ok: false,
  code: 'VALIDATION_FAILED',
  message: '预算金额不能为空'
}
```

禁止用异常做正常业务流程控制。

允许异常的场景：
- 编程错误
- 配置缺失
- 数据库不可用
- 网络中断
- 外部系统协议异常
- 不可恢复的系统错误

边界层必须把异常转换为统一错误响应、日志事件或用户可理解的错误提示。

### 2.4 核心逻辑默认不可变

核心逻辑不得原地修改传入对象或数组。

错误示例：

```js
function approveTender(tender) {
  tender.status = 'APPROVED'
  return tender
}
```

正确示例：

```js
function approveTender(tender) {
  return {
    ...tender,
    status: 'APPROVED'
  }
}
```

Vue 组件和 Pinia Store 可以进行受控状态更新，但更新前的业务计算应尽量放到纯函数中完成。

### 2.5 FP-Java Profile 可执行门禁

后端使用 `FPJavaArchitectureTest` 将 FP-Java Profile 变成自动门禁。

适用范围：
- `com.xiyu.bid..core..`
- `com.xiyu.bid..domain..`
- 排除 `..entity..`，因为 JPA Entity 受 ORM 框架约束，允许可变状态和无参构造等例外。

门禁内容：
- 纯核心不得依赖 Controller、Repository、Config、Adapter、Gateway。
- 纯核心不得依赖 Spring Web/Data/JDBC、JPA、日志、文件、网络等命令式外壳或 I/O API。
- 纯核心不得依赖项目业务异常包；预期业务失败应通过 Result / Optional / ValidationResult 返回。
- 纯核心业务方法不得返回 `void`；状态变化必须通过返回值表达。
- 纯核心业务方法不得声明、构造或捕获异常来表达业务流程。
- 纯核心数据默认使用 record 或 final 字段，不暴露 setter。

执行命令：

```bash
cd /Users/user/xiyu/xiyu-bid-poc/backend
mvn test -Dtest=FPJavaArchitectureTest
```

说明：
- 这是面向新增纯核心包的硬门禁，不会把当前存量 DTO、Service、JPA Entity 一次性纳入红灯区。
- 如果某段逻辑需要被 FP-Java Profile 保护，应主动迁入 `core` / `domain` 非 Entity 包，而不是继续留在事务编排 Service 中。

### 2.6 Split-First 可执行门禁

后端使用 `MaintainabilityArchitectureTest` 约束受保护模块中的 `Service` 形状，阻止新功能第一次落地就长成上帝类。

当前受保护模块：
- `calendar`
- `collaboration`
- `competitionintel`
- `scoreanalysis`
- `roi`
- `versionhistory`
- `documenteditor`
- `documents`

门禁内容：
- 受保护模块的 `Service` 文件默认不得超过 `300` 行。
- 受保护模块的 `Service` 默认不得依赖超过 `5` 个实例协作者。
- 受保护模块的 `Service` 默认不得暴露超过 `8` 个公开方法。

执行命令：

```bash
cd /Users/user/xiyu/xiyu-bid-poc/backend
mvn test -Dtest=MaintainabilityArchitectureTest
```

说明：
- 这是棘轮式门禁，不是假装全仓已经收口。当前少量超标的历史 `Service` 先保留小范围白名单，后续重构时逐个摘除。
- 新增代码不允许以“历史上也很大”为理由继续扩散；一旦进入受保护模块，就必须按 Split-First Rule 拆分。

---

## 3. Mock 政策（统一决策）

### 3.1 唯一事实源

- 项目从当前时点起，统一按**真实后端 API 单一路径**执行。
- 前端页面、组件、Store、路由、E2E、演示脚本均不得把 Mock 作为可选正常路径。
- 新功能、新页面、新接口联调一律不允许增加双模式判断。

### 3.2 对存量遗留的处理原则

- 仓库内仍保留 `frontendDemo` 适配层、`demoPersistence` 等历史遗留。
- 这些内容的身份是：**待删除技术债**，不是允许继续沿用的架构设计。
- 遇到相关代码时，允许在本次任务范围内继续清理；不允许新增依赖、不允许扩散使用面、不允许恢复成默认链路。

### 3.3 禁止事项

- 禁止新增任何以 `mock`/`demo` 为条件的数据分支。
- 禁止新增从本地静态假数据读取业务数据的逻辑。
- 禁止以“联调未完成”为由添加本地假数据兜底。
- 禁止把历史 `mock` 命令写回 README、演示流程或测试基线。

---

## 4. 当前硬门禁与执行口径

### 4.1 当前可直接执行的前端门禁

以下命令当前在仓库中可执行，并应作为前端与文档的硬门禁：

```bash
npm run check:front-data-boundaries
npm run check:doc-governance
npm run build
```

说明：
- `npm run build` 当前会串行执行前两条门禁后再进行 Vite 构建。
- 这些门禁当前是可信的，但覆盖范围主要在前端边界与文档治理，不代表后端架构已完全收口。

### 4.2 当前后端验证口径

建议命令：

```bash
cd backend
mvn test -Dtest=<相关测试类>
mvn test -Dtest=ArchitectureTest
mvn test
```

当前现状：
- `ArchitectureTest` 已存在并能运行，当前基线已恢复为**全绿**。
- 截至 2026-04-16，历史上的两类失败已完成修复：
  - `config -> service` 违规依赖：`E2eDemoDataInitializer` 不再直接依赖 `RoleProfileService`
  - `config <-> service` 循环依赖：`RateLimitService` 不再依赖 `ExportConfig`
- 因此，当前后端验证要求是：
  - 至少运行与本次变更相关的测试
  - 若触及架构边界，运行 `ArchitectureTest`
  - 若新增或修改 `..core..` / `..domain..` 非 Entity 纯核心代码，运行 `FPJavaArchitectureTest`
  - 若新增或扩展受保护模块的 `Service`，运行 `MaintainabilityArchitectureTest`
  - 如出现失败，按新引入问题处理
  - 不得再把当前仓库写成“存在已知存量失败”

---

## 5. 架构边界

### 5.1 后端分层原则

目标分层仍然是：

```text
Controller → Service → Repository → Entity
```

当前执行要求：
- 新代码不得让 Controller 直接访问 Repository。
- 新代码不得继续制造 `config -> service` 或 `service -> config` 循环。
- 新业务优先放入按业务域分包的结构中，不再把新能力塞回根层平铺包。
- 如果为了兼容存量代码临时触碰例外，必须在结论中明确指出，不得伪装成规范实现。

### 5.2 前端边界

- 页面、组件、Store 不得直接 import `@/api/mock`。
- 页面、组件、Store 不得直接 import `@/utils/demoPersistence`。
- 页面、组件、Store 不得新增本地 `mockData` 作为失败兜底。
- 路由、Store、API 模块中现存的 Mock 遗留判断属于待清理项，本次之后不允许继续扩散。

### 5.3 关于自动检查脚本的真实能力

- `scripts/check-front-data-boundaries.mjs` 当前会扫描 `views`、`components`、`stores` 下的部分直接违规导入与本地 `mockData` 定义。
- 该脚本**当前还不能完整识别**所有 `isMockMode()` 或 API 模块内部的双模式遗留。
- 因此，代码审查仍需人工补位，不能把脚本通过等同于“Mock 已彻底清理完毕”。

---

## 6. 安全要求

### 6.1 当前必须遵守的要求

- 除白名单外的 API 默认需要认证。
- 管理端点 `/api/admin/**` 必须要求管理员角色。
- 密码必须使用 BCrypt 存储。
- 用户输入必须经过清洗与校验后再入库或参与业务处理。
- 禁止硬编码凭据、密钥、Token。
- 禁止在日志中输出密码、Token 原文和完整敏感身份信息。

### 6.2 当前代码与目标策略的差异

当前 `SecurityConfig` 中还存在以下现状：
- 白名单实际额外包含 `/api/auth/sessions`、`/actuator/info`、`/h2-console/**`
- 默认 CORS 仍允许 `5173`、`5174`、`3000` 等历史开发端口

这些属于当前代码事实，不代表目标生产策略。
在安全配置尚未进一步收口前：
- 不得继续扩大白名单
- 不得继续扩大允许的默认跨域来源
- 如需调整，必须同时更新文档与配置，并说明影响面

---

## 7. 端口与环境约定

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 1314 | 默认开发与演示端口 |
| 后端 | 18080 | Spring Boot API 服务 |

补充说明：
- 根目录 `npm run dev:all` 会以真实 API 模式启动前后端。
- 根目录 `start.sh` 当前会给后端注入 `SPRING_PROFILES_ACTIVE=e2e`，并给前端注入 `VITE_API_MODE=api`。
- `14173` 等临时端口只能用于短时排查，不作为项目口径写回文档、截图、测试或演示说明。

---

## 8. 文档同步要求

- 代码变更必须同步更新受影响的 README、规则说明和使用入口文档。
- 文档中必须区分“当前已实现事实”和“目标策略/待清理事项”，禁止继续写成混淆状态。
- 文档治理检查命令 `npm run check:doc-governance` 必须通过。

---

## 9. 当前基线摘要（2026-04-16）

- 前端边界检查：可通过
- 文档治理检查：可通过
- 前端生产构建：可通过
- 后端 ArchitectureTest：已修复并恢复为全绿基线
- Mock 政策：已统一决策为“彻底删除双模式”，但代码层面仍有遗留待清理
