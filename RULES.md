# RULES.md — 项目强制红线与开发作业流程

一旦我所属的文件夹有所变化，请更新我。

本文件定义所有参与本项目开发的人类与 AI 代理必须遵守的**强制红线**。
违反任何一条红线的变更，不得进入主干分支。

---

## 1. 标准作业流程（SOP）

所有功能开发、Bug 修复、重构任务必须按以下四阶段执行，不得跳过或合并阶段。

### Phase 1: Plan（规划）

- 开始写代码前，必须先理解需求并产出实施计划。
- 计划必须明确：改动范围（哪些文件）、验收标准（什么算完成）、风险点。
- 复杂任务（跨 3 个以上文件 / 涉及架构变更）必须形成书面计划并获得确认后才能进入下一阶段。
- 禁止"边写边想"，禁止在没有明确目标的情况下开始修改生产代码。

### Phase 2: TDD（测试驱动）

- 先写测试，再写实现。遵循 Red → Green → Refactor 循环。
- **Red**：编写一个会失败的测试，验证测试确实因为"功能未实现"而失败。
- **Green**：编写满足测试通过的最少代码，禁止过度工程。
- 每个 Phase 结束后必须运行完整测试套件，确认无回归。
- 后端：`mvn test`；前端：`npm run build`（含门禁）；E2E：`npm run test:e2e`。

### Phase 3: Code Review（代码审查）

- 实现完成后，必须以"高级架构师"视角对变更进行自审。
- 审查清单：
  - [ ] 是否违反分层架构（Controller → Service → Repository）？
  - [ ] 是否引入反向依赖或循环依赖？
  - [ ] 是否存在安全风险（未校验输入、硬编码凭据、SQL 注入）？
  - [ ] 是否符合文档治理规范（头注释、README 同步）？
  - [ ] 是否仍残留旧逻辑阻碍真实的底层数据流转？
- 发现问题必须在本阶段修复，不得"先合并再修"。

### Phase 4: Refactor-Clean（重构与清理）

- 在所有测试通过的前提下，进行代码清理和优化。
- 消除重复代码、改善命名、提取公共方法。
- 确保文档同步更新（README.md、头注释）。
- 重构后必须再次运行全量测试，确认零回归。
- 提交前运行门禁：`npm run check:front-data-boundaries && npm run check:doc-governance`。

---

## 2. 架构红线

### 2.1 分层依赖方向（后端）

```
Controller → Service → Repository → Entity
     ↓           ↓
    DTO         DTO
```

- **Controller 禁止直接依赖 Repository**。必须通过 Service 层访问数据。
- **Controller 禁止直接依赖 Entity**（新模块强制，老模块逐步迁移）。
- **Service 禁止依赖 Controller**。
- **Entity 禁止依赖 Service 和 Controller**。必须是纯领域模型。
- **DTO 禁止依赖 Service**。
- **Config 禁止依赖 Service**。
- **Util / Helper 禁止依赖 Service 和 Repository**。必须是无状态纯函数。
- 以上规则由 `ArchitectureTest.java`（ArchUnit）在构建时强制执行。

### 2.2 模块隔离（后端）

- 新业务域（`calendar`、`collaboration`、`competitionintel`、`scoreanalysis`、`roi`、`versionhistory`、`documenteditor`、`documents`）之间禁止相互依赖。
- 禁止循环依赖（所有模块）。由 ArchUnit `slices().should().beFreeOfCycles()` 强制。
- 新代码只允许进入"按业务域分包"结构（如 `com.xiyu.bid.tender.*`）。根包下的平铺层（`controller/service/dto/repository/entity`）已冻结，不再新增功能类。

### 2.3 分层边界（前端）

- **页面/组件/Store 禁止直接 import `@/api/mock`**。
- **页面/组件/Store 禁止直接 import `@/utils/demoPersistence`**。
- 禁止任何形态的前端 Mock 兜底，所有界面数据必须严格来源于真实后端的下发。
- 组件一旦侦测到接口异常，应直接抛给用户真实的错误状态，绝不应当被悄悄吃掉或使用静态数据掩盖。
- 以上规则由 `scripts/check-front-data-boundaries.mjs` 在构建时强制执行。

---

## 3. 安全红线

### 3.1 认证与授权

- 所有 API 端点默认需要认证（`anyRequest().authenticated()`）。
- 白名单端点仅限于：`/api/auth/login`、`/api/auth/register`、`/api/auth/logout`、`/api/auth/refresh`、`/api/auth/forgot-password`、`/api/auth/reset-password`、`/api/auth/verify-email/**`、`/api/public/**`、`/actuator/health`。
- 管理端点（`/api/admin/**`）必须 `hasRole('ADMIN')`。
- 会话管理使用无状态 JWT + Refresh Token 轮换机制。
- Refresh Token 必须 SHA-256 哈希后存储，禁止明文持久化。

### 3.2 输入处理

- 所有用户输入（用户名、邮箱、备注、文件名等）必须经过 `InputSanitizer.sanitizeString()` 清洗后才能使用。
- 密码必须经过 `PasswordValidator.validate()` 强度校验。
- 密码必须使用 BCrypt 编码存储，禁止明文或可逆加密。
- 文件名必须经过正则清洗（`[^a-zA-Z0-9._-]` → `_`），防止路径穿越。

### 3.3 CORS

- CORS 仅允许配置的前端域名（默认：`localhost:1314`、`127.0.0.1:1314`）。
- 禁止使用 `*` 通配符作为 `allowedOrigins`。
- `allowedHeaders` 必须显式列举，禁止通配符。

### 3.4 禁止事项

- **禁止硬编码**任何凭据、密钥、Token 到源码中。
- **禁止关闭** CSRF 防护而不补偿（当前使用无状态 JWT，CSRF 已通过架构补偿）。
- **禁止**在日志中输出密码、Token 原文或完整身份信息。
- **禁止**在生产环境暴露 H2 Console、`/actuator` 全量端点、调试端口。

---

## 4. 前端禁忌

| 编号 | 禁忌 | 原因 |
|------|------|------|
| F-01 | 页面/组件内定义本地 `mockData` 作为失败态 fallback | 掩盖底层真实问题 |
| F-02 | 尝试恢复已被废弃的 `isMockMode()` 判断逻辑 | 破坏交付架构红线 |
| F-03 | 交付冲刺期依然伪造"成功但其实是硬编码"的响应 | 数据结果不可信 |
| F-04 | 使用 `14173` 等临时端口作为项目约定或测试基线 | 端口约定统一为 `1314` |
| F-05 | 新增前端功能不经过 `npm run check:front-data-boundaries` | CI扫描门禁失效 |
| F-06 | Store 层直接向 UI 层拼接递送固化 Demo 数据 | 违反分层治理边界 |

---

## 5. 后端禁忌

| 编号 | 禁忌 | 原因 |
|------|------|------|
| B-01 | Controller 注入 `*Repository` | 穿透 Service 层 |
| B-02 | 通过 Service getter 暴露 Repository（如 `getUserRepository()`）| 变相穿透 |
| B-03 | 在旧平铺包新增业务类 | 冻结区已锁定 |
| B-04 | 同一 DTO 在多个包中出现同名定义 | 导包混乱 |
| B-05 | Controller 直接使用 `EntityManager` 或 `SessionFactory` | 绕过 Repository |
| B-06 | 新增白名单端点不更新 `SecurityConfig.WHITE_LIST_URL` | 安全缺口 |
| B-07 | 跳过 `InputSanitizer` 直接使用用户输入 | 注入风险 |

---

## 6. 文档同步红线

- 代码变更必须同步更新受影响的 README.md 和源码头注释。
- 头注释格式：`Input / Output / Pos / 维护声明`（Java）或 `Input / Output / Pos / 更新提示`（JS/Shell）。
- 构建门禁 `npm run check:doc-governance` 必须通过。
- 详细规范见 [`docs/DOCUMENTATION_GOVERNANCE.md`](docs/DOCUMENTATION_GOVERNANCE.md)。

---

## 7. 环境与端口约定

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | **1314** | 全局唯一约定的前端开发端口 |
| 后端 | **18080** | Spring Boot API 服务 |

- 系统开发全面处于交付期的真实 API 模式。
- 前端默认 API 基础地址绑定为：`http://localhost:18080`。

---

## 8. 构建门禁清单

所有变更在合并前必须通过以下检查：

```bash
# 前端
npm run check:front-data-boundaries   # 防止遗留Demo数据污染扫描
npm run check:doc-governance           # 文档治理检查
npm run build                          # 生产构建（自动包含上述两项）

# 后端
mvn compile                            # 编译检查
mvn test -Dtest=ArchitectureTest       # 架构规则强制
mvn test                               # 全量单元测试

# E2E
npm run test:e2e                       # 端到端测试
```

---

## 9. 版本与变更记录

| 日期 | 变更 | 作者 |
|------|------|------|
| 2026-04-16 | 初始版本：四阶段 SOP、架构/安全/前后端红线 | 项目组 |
