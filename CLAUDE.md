# CLAUDE.md

本文件提供本仓库的执行入口、常用命令、验证清单和环境坑点。
它的目标不是描述理想状态，而是帮助代理和开发者快速对齐当前仓库的真实情况。

## 仓库路径

- 项目根目录：`/Users/user/xiyu/xiyu-bid-poc`
- 后端目录：`/Users/user/xiyu/xiyu-bid-poc/backend`

## 当前项目口径

- 对外项目名称统一为“西域数智化投标管理平台”。
- 仓库名、包名、构件名中的 `xiyu-bid-poc`、`bid-poc` 属于历史遗留。
- 当前项目按真实 API 交付模式协作，不再把 Mock 当作正常路径。
- 仓库中仍保留少量 `frontendDemo`、`demoPersistence` 等遗留内容；这些只代表待清理技术债，不代表允许继续使用的架构策略。

## 推荐命令

### 启动

```bash
# 推荐：一键联调
cd /Users/user/xiyu/xiyu-bid-poc
npm run dev:all
```

```bash
# 手动方式：后端
cd /Users/user/xiyu/xiyu-bid-poc/backend
# 推荐：使用 start.sh（已内置默认环境变量）
./start.sh

# 或直接使用 mvn（需手动传入必需环境变量）
JWT_SECRET="xiyu-bid-poc-local-dev-secret-key-please-change-in-prod-32bytes-min" \
DB_PASSWORD="XiyuDB!2026" \
CORS_ALLOWED_ORIGINS="http://localhost:1314,http://127.0.0.1:1314" \
mvn spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--server.port=18080"
```

> 后端必需环境变量（未设置会导致启动失败）：
> - `JWT_SECRET`：JWT 签名密钥（至少 32 字节），`backend/start.sh` 已提供本地默认值
> - `DB_PASSWORD`：MySQL 8.0 密码（默认 `XiyuDB!2026`）
> - `CORS_ALLOWED_ORIGINS`：允许的前端源地址，默认包含 `http://localhost:1314` 与 `http://127.0.0.1:1314`
>
> 生产部署必须通过真实环境注入这些值，**不得依赖上述默认值**。

```bash
# 手动方式：前端（真实 API 模式）
cd /Users/user/xiyu/xiyu-bid-poc
VITE_API_MODE=api VITE_API_BASE_URL=http://127.0.0.1:18080 npm run dev -- --host 127.0.0.1 --port 1314
```

### 前端与文档验证

```bash
cd /Users/user/xiyu/xiyu-bid-poc
npm run check:front-data-boundaries
npm run check:doc-governance
npm run check:line-budgets
npm run build
npm run test:unit
npm run test:e2e
```

### 后端验证

```bash
cd /Users/user/xiyu/xiyu-bid-poc/backend
mvn test -Dtest=<相关测试类>
mvn test -Dtest=ArchitectureTest
mvn test
```

## 当前验证清单（按可信度排序）

### 1. 当前可直接信任的前端基线

以下命令截至 2026-04-22 当前可通过：

```bash
npm run check:front-data-boundaries
npm run check:doc-governance
npm run check:line-budgets
npm run build
npm run test:unit
npm run test:e2e
```

### 2. 后端验证口径

- `ArchitectureTest` 已恢复为**全绿基线**。
- 2026-04-16 已完成两类历史问题修复：
  - `E2eDemoDataInitializer` 引发的 `config -> service` 违规依赖
  - `RateLimitService` 与 `ExportConfig` 引发的 `config <-> service` 循环依赖
- 因此，后端任务完成时必须：
  - 跑受影响测试
  - 如涉及架构边界，再跑 `mvn test -Dtest=ArchitectureTest`
  - 如出现失败，按新增问题处理并说明影响范围
  - 不得再把当前仓库写成“存在已知存量失败”

## 默认登录凭据

| 环境 | 用户名 | 密码 | 角色 | 来源 |
|------|--------|------|------|------|
| dev / prod | `admin` | `XiyuAdmin2026!` | ADMIN | V57 迁移 + DefaultAdminInitializer |
| e2e | `lizong` | `123456` | ADMIN | E2eDemoDataInitializer |
| e2e | `zhangjingli` | `123456` | MANAGER | E2eDemoDataInitializer |
| e2e | `xiaowang` | `123456` | STAFF | E2eDemoDataInitializer |

生产环境通过 `ADMIN_PASSWORD` 环境变量覆盖默认密码。任何 profile 启动后数据库至少有一个可登录账户。

## 端口约定

- 前端开发与演示统一使用 `1314`
- 后端 API 统一使用 `18080`
- 默认访问地址：`http://127.0.0.1:1314`
- 默认后端健康检查：`http://127.0.0.1:18080/actuator/health`

## 环境坑点

1. **`npm run dev` 只会启动前端**
   如果任务需要真实链路联调，优先使用 `npm run dev:all`，不要误以为单独前端已经代表系统启动完成。

2. **根目录 `start.sh` 会强制真实 API 模式**
   当前脚本会给后端注入 `SPRING_PROFILES_ACTIVE=dev,mysql`，给前端注入 `VITE_API_MODE=api` 和 `VITE_API_BASE_URL=http://127.0.0.1:18080`。

3. **后端默认端口不是 8080，而是 18080**
   当前文档、脚本、E2E 和联调路径都以 `18080` 为准。

4. **仓库仍有 Mock 遗留，但不应继续使用**
   `src/api/config.js`、部分 API 模块、部分 Store 和路由里仍有双模式痕迹；它们是历史技术债，不是允许继续依赖的路径。

5. **`check-front-data-boundaries` 不是全能扫描器**
   它能拦一部分明显违规导入，但还不能覆盖所有 `isMockMode()` 或 API 模块内部的双模式遗留；代码审查时仍需人工检查。

6. **安全配置当前比目标生产策略更宽松**
   当前 `SecurityConfig` 仍放行 `/api/auth/sessions`、`/actuator/info`、`/h2-console/**`，默认 CORS 也兼容若干历史开发端口。不要继续扩大这些范围；如需调整，必须同步文档与代码。

7. **仓库命名仍带 `POC`**
   `package.json`、`pom.xml` 中仍使用 `poc` 命名，这是历史遗留。对外表达、汇报和文档正文不要继续强化 POC 口径。

8. **后端启动必须提供必需环境变量**
   直接运行 `mvn spring-boot:run` 会因为缺少 `JWT_SECRET`、`DB_PASSWORD` 而启动失败。
   本地开发推荐使用 `backend/start.sh`（已内置默认值），或参考“推荐命令 / 手动方式：后端”传入完整环境变量。
   生产部署必须通过真实环境注入，**不得使用 `start.sh` 中的本地默认值**。

## 路径提示

- 前端业务代码：`src/`
- 后端业务代码：`backend/src/main/java/com/xiyu/bid/`
- 后端启动初始化：`backend/src/main/java/com/xiyu/bid/bootstrap/`（独立于 config 包，避免 ArchitectureTest RULE 9）
- 后端测试：`backend/src/test/java/com/xiyu/bid/`
- E2E：`e2e/`
- 标书生成 Agent：`backend/src/main/java/com/xiyu/bid/biddraftagent/`
- 文档编辑器草稿树写入：`backend/src/main/java/com/xiyu/bid/documenteditor/`
- 治理脚本：`scripts/`
- 交付与规范文档：`docs/`
- 项目知识库（Wiki）：`.wiki/pages/`（含标书需求追溯、架构、模块、缺口分析等，导航见 `.wiki/pages/_index.md`）

## 执行原则

- 真实 API 是唯一支持路径。
- 核心业务逻辑遵守 `RULES.md`：纯核心与命令式外壳分离，业务错误优先作为值返回，核心计算默认不原地修改输入。
- 文档要反映“当前事实 + 待清理事项”，不要再把目标状态写成现状。
- 发现架构测试、Mock 遗留或安全配置与文档不一致时，应优先修正文档口径，或在同次任务中同步收口代码，而不是继续掩盖。 

---

## 多 Agent 执行手册 (SOP 落地)

### 🚨 核心指令：进入工作区后的“早操” (必做)
**在进行任何代码修改前，你必须确保你的代码库是最新的：**
```bash
git fetch origin && git rebase origin/main && ./scripts/sync-env.sh .
```

### 1. 快速进入开发状态
1. **确认路径**：确保你位于 `/Users/user/xiyu/worktrees/[Agent名称]`。
2. **同步环境**：在 Worktree 根目录下执行 `./scripts/sync-env.sh .`。
3. **环境检测**：执行 `source scripts/dev-env.sh`。

### 2. 专属资源映射表
| Agent | 前端端口 | 后端端口 | 数据库名 | Redis DB |
| :--- | :--- | :--- | :--- | :--- |
| **Claude** | 1315 | 18081 | xiyu_bid_claude | 1 |
| **Codex** | 1316 | 18082 | xiyu_bid_codex | 2 |
| **Gemini** | 1317 | 18083 | xiyu_bid_gemini | 3 |
| **Cursor** | 1318 | 18084 | xiyu_bid_cursor | 4 |
| **Integrator** | 1319 | 18085 | xiyu_bid_integrator | 5 |

### 3. 协作启动命令
Agent 必须使用包装脚本启动，以自动适配上述隔离端口：
- **启动前端**：`./scripts/start-frontend.sh`
- **启动后端**：`./scripts/start-backend.sh`

### 4. 任务完成门禁
在报告完成前，必须在 **当前 Worktree** 运行：
1. `npm run build` (前端构建验证)
2. `cd backend && mvn test` (后端全量/受影响测试验证)
3. `git status` 确认只修改了授权文件。

### 5. 任务启动协议 (Lease + Auto-Detect)
不画静态目录所有权表 — 任务推进就过时。改用 **git 事实** 当主信号，Gemini 的 conductor 任务声明当辅助。

#### 5.0 同步基线（**每次开新任务都要跑**，不只是 session 开头）
"早操"只覆盖 session 开头。**任务之间也必须重新同步** — 否则 5.1 的 `who-touches.sh` 看到的是旧 main 的 diff，可能漏掉别的 agent 中途合的改动，你照样会在过期 base 上累工作。

开新任务前必跑：
```bash
git fetch origin && git rebase origin/main
```
- 没改动 → no-op（2-3 秒）
- 有改动 → 早暴露 conflict，不留给 merge 时再处理
- **uncommitted 工作中途要 sync** → 先 `git stash` 保护现场，rebase 后再 `git stash pop`

5.1 / 5.2 必须在 5.0 之后跑（否则 git 数据是旧的）。可以用 alias 把两步合一：
```bash
alias agent-start='git fetch origin && git rebase origin/main'
# 然后日常：agent-start && ./scripts/who-touches.sh <path>
```

> **同步频率 mental model**：sync **不是"每天一次的事"**，是 **"开始做新工作之前的最后一道清醒动作"**。session 开头 / 任务起点 / 推 PR 前 — 这三个节点必跑；其它时刻原则上不要在 uncommitted 工作中途 rebase。

#### 5.1 主信号：git 事实（**所有 agent 通用，必跑**）
开新任务前对你打算改的路径跑：
```bash
./scripts/who-touches.sh <path-or-glob>
```
列出有未合 commit 的 `agent/*` 分支（origin + local 去重，跳过自己）。
- 退出码 `0` + 无输出 → 干净，可以开工
- 退出码 `1` + 有输出 → 别的 agent 在动这块，看清楚再决定

#### 5.2 辅助信号：Gemini 的任务声明（**仅当撞到 `agent/gemini-init` 时有用**）
Gemini 在 `conductor/tracks/` 系统里执行任务，会把 in-progress 任务标 `[~]` 并附 `(@gemini, scope: ...)`。如果 5.1 显示 `agent/gemini-init` 在你的目标路径有未合改动，可以查一眼具体任务上下文：
```bash
grep -h "\[~\]" conductor/tracks/*/plan.md | grep gemini
```

> **重要**：Claude / Codex / Cursor **没有等价的任务声明机制**。撞到这几个 agent 的分支时，**不要假设可以从 plan.md 查到他们的意图** — 直接看 `git log <branch>` 的 commit message，或在 PR 描述里 @ 对方协调。其他 agent 的"意图声明"靠 §6 的 commit message + push 频率自然沉淀。

#### 5.3 撞了的处置
- 等对方 push 完一个原子 commit / PR merge / Gemini 任务标 `[x]`
- 换一个不撞的任务先做
- 在 PR 描述里 @ 对方说明协调结果（"我接着你的 X 改 Y，rebase 你的 PR 后再合"）

#### 5.4 没撞 → 开工
- 你是 Gemini：在 plan.md 把任务从 `[ ]` 改成 `[~] (@gemini, scope: ...)` 让别人看见你的意图
- 你是其他 agent：**push commit 时就是你的意图声明** — commit message 写清楚 scope，下个 §6

> 验证脚本：`./scripts/who-touches.sh --self-test` 应该看到 4 个 `[PASS]`。

### 6. 纪律：每日 push WIP 分支
`who-touches.sh` 是所有 agent 共用的协调机制，准确性靠每天至少 push 一次自己 `agent/*` 分支：
- **本地 commit 不算数** — 别的 agent / session 看不到，lease 检测会漏。
- 每个工作 session 结束前推一次（即使没开 PR、即使是半成品）：
  ```bash
  git push origin HEAD:$(git rev-parse --abbrev-ref HEAD)
  ```
- push 是给 lease 检测看的，**不是为了 merge** — 半成品 WIP 分支允许、欢迎、必须存在。

> 对非 Gemini 的 agent 而言，**commit message + 改动文件就是你的"我在做这事"声明**。所以 commit message 要明确，例如 `wip: add CONTRACT profile (scope: docinsight/contract*)`，即使没 PR 别人也能从 `who-touches.sh` 输出 + `git log <branch> --oneline -5` 推断你在干什么。

