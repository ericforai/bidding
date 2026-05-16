<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

# CLAUDE.md

每次对话开始，都请打招呼：同志，你好！
本文件提供本仓库的执行入口、常用命令、验证清单和环境坑点。
它的目标不是描述理想状态，而是帮助代理和开发者快速对齐当前仓库的真实情况。

## 仓库路径

- 项目根目录：`/Users/user/xiyu/xiyu-bid-poc`
- 后端目录：`/Users/user/xiyu/xiyu-bid-poc/backend`

## 当前项目口径

- 对外项目名称统一为“西域数智化投标管理平台”。
- 仓库名、包名、构件名中的 `xiyu-bid-poc`、`bid-poc` 属于历史遗留。
- 当前项目按真实 API 交付模式协作，Mock 模式已于 2026-04-30 退役（`mock.js`、`mock-adapters/`、`.env.mock` 均已删除）。
- 如仍在其它文档或评论中看到 `frontendDemo` / `demoPersistence` / `isMockMode` 字样，视为过期表述，不代表仓库真实状态。
- **数据库**：仅支持 MySQL 8.0。迁移脚本统一放在 `migration-mysql/` 目录，`migration/` 目录已废弃并删除。

### 数据库迁移规范

- **迁移脚本位置**：`backend/src/main/resources/db/migration-mysql/`
- **命名规范**：
  - 基线版本：`B{version}_*.sql`（如 `B73__full_schema_baseline.sql`）
  - 增量版本：`V{version}_*.sql`（如 `V114__tender_source_type.sql`）
- **版本号**：必须大于已有最大版本号
- **回滚脚本**：放在 `db/rollback/` 目录，与迁移脚本版本对应

## 推荐命令

### 启动

> **Dev-only guard**：`backend/start.sh`、`scripts/dev-services.sh`、`scripts/dev-services-launchd.sh`、`scripts/local-docker-stack.sh`、`scripts/release/rehearsal-env.sh` 均内置 dev-only 双层守卫。
> 必须显式导出 `XIYU_DEV_CONFIRMED=1` 才能运行，且任一 `SPRING_PROFILES_ACTIVE` / `XIYU_ENV` / `NODE_ENV` / `ENV` / `ENVIRONMENT` 含有 `prod*`、`production`、`staging`、`stg`、`release`、`live`、`uat`、`canary` 等信号时均会拒绝执行。
> 本地跑：`export XIYU_DEV_CONFIRMED=1` 再调用脚本；生产部署不得走这些脚本。

```bash
# 推荐：一键联调
cd /Users/user/xiyu/xiyu-bid-poc
export XIYU_DEV_CONFIRMED=1
npm run dev:all
```

```bash
# 手动方式：后端
cd /Users/user/xiyu/xiyu-bid-poc/backend
# 推荐：使用 start.sh（已内置默认环境变量，需 XIYU_DEV_CONFIRMED=1）
XIYU_DEV_CONFIRMED=1 ./start.sh

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

### dev / prod

| 用户名 | 密码 | 角色 | 来源 |
|--------|------|------|------|
| `admin` | `XiyuAdmin2026!` | `admin` (ADMIN) | V57 迁移 + DefaultAdminInitializer |

### e2e

| 用户名 | 密码 | RoleProfile | Legacy 角色 | 来源 |
|--------|------|-------------|-------------|------|
| `lizong` | `123456` | `admin` | ADMIN | E2eDemoDataInitializer |
| `zhangjingli` | `123456` | `manager` | MANAGER | E2eDemoDataInitializer |
| `xiaowang` | `123456` | `staff` | STAFF | E2eDemoDataInitializer |
| `xiaochen` | `123456` | `bid_admin` | MANAGER | E2eDemoDataInitializer |
| `xiaoliu` | `123456` | `bid_lead` | MANAGER | E2eDemoDataInitializer |
| `xiaozhang` | `123456` | `sales` | MANAGER | E2eDemoDataInitializer |
| `xiaozhou` | `123456` | `bid_specialist` | STAFF | E2eDemoDataInitializer |
| `xiaozheng` | `123456` | `admin_staff` | STAFF | E2eDemoDataInitializer |

### dev (本地联调，SPRING_PROFILES_ACTIVE=dev)

| 用户名 | 密码 | RoleProfile | Legacy 角色 | 来源 |
|--------|------|-------------|-------------|------|
| `staff` | `Test@123` | `staff` | STAFF | LocalDevAccountInitializer |
| `manager` | `Test@123` | `manager` | MANAGER | LocalDevAccountInitializer |
| `bid_admin` | `Test@123` | `bid_admin` | MANAGER | LocalDevAccountInitializer |
| `bid_lead` | `Test@123` | `bid_lead` | MANAGER | LocalDevAccountInitializer |
| `sales` | `Test@123` | `sales` | MANAGER | LocalDevAccountInitializer |
| `bid_specialist` | `Test@123` | `bid_specialist` | STAFF | LocalDevAccountInitializer |
| `admin_staff` | `Test@123` | `admin_staff` | STAFF | LocalDevAccountInitializer |

> `bid_specialist` 与 `admin_staff` 为 2026-05-16 新增角色，对应产品蓝图 "投标专员" 与 "行政人员"。

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

4. **Mock 模式已退役（2026-04-30）**
   `src/api/mock.js`、`src/api/mock-adapters/`、`.env.mock` 均已删除；`src/api/config.js` 硬编码 `mode: 'api'`，不再读取 `VITE_API_MODE`。旧文档里的"双模式切换"/`isMockMode()` 路径均为已退役的历史表述，不要再把它们当作现状。

5. **`check-front-data-boundaries` 不是全能扫描器**
   它拦一部分明显违规导入（如直接 import 已删除的 mock 模块），但不能覆盖所有前端数据边界；代码审查时仍需人工检查。

6. **安全配置当前比目标生产策略更宽松**
   当前 `SecurityConfig` 仍放行 `/api/auth/sessions`、`/actuator/info`、`/h2-console/**`，默认 CORS 也兼容若干历史开发端口。不要继续扩大这些范围；如需调整，必须同步文档与代码。

7. **仓库命名仍带 `POC`**
   `package.json`、`pom.xml` 中仍使用 `poc` 命名，这是历史遗留。对外表达、汇报和文档正文不要继续强化 POC 口径。

8. **后端启动必须提供必需环境变量**
   直接运行 `mvn spring-boot:run` 会因为缺少 `JWT_SECRET`、`DB_PASSWORD` 而启动失败。
   本地开发推荐使用 `backend/start.sh`（已内置默认值），或参考“推荐命令 / 手动方式：后端”传入完整环境变量。
   生产部署必须通过真实环境注入，**不得使用 `start.sh` 中的本地默认值**。

9. **launchd 守护进程会在后台自动重启 dev-services**
   `~/Library/LaunchAgents/com.xiyu.bid.dev-services.{main,codex,gemini,claude}.plist` 会在登录时自动启动 `scripts/dev-services.sh watch-run`。`pkill` 杀不掉它们，launchd 会立即重启。要彻底停某一个，用 `launchctl bootout gui/$(id -u)/com.xiyu.bid.dev-services.<label>`。

10. **watchdog 后端失败 10 次会进入 STOPPED 状态**
    新版 `scripts/dev-services.sh` 给 backend 重启加了指数退避（30s → 2min → 10min → 30min cap）。连续失败 10 次后写入 `.runtime/dev-services/backend.fail-state` 并停止重试。`scripts/dev-services.sh start` 在 fail-state 存在时会拒绝启动并打印最后的错误行。修复后用 `rm .runtime/dev-services/backend.fail-state && ./scripts/dev-services.sh start` 恢复。
    可调整：`WATCHDOG_BACKEND_MAX_FAILURES` 环境变量（默认 10）。
    **全局聚合**：`npm run agent:health-check` 会扫描所有 worktree 的 `.runtime/dev-services/` 并打印总体状况（每个 worktree 的 backend/frontend/sidecar 是否 ALIVE、最近一条 ERROR 行、fail-state 详情）。怀疑某个 worktree 在闷头重启时先跑一下这个。

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
不画静态目录所有权表 — 任务推进就过时。改用 **git 事实** 当主信号，文件锁仅用于 hot-paths 前置预订。

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

#### 5.2 文件锁（hot-paths 前置预订）— 已改为 per-task 文件
`scripts/hot-paths.yml` 列出的高危路径（DB 迁移、entity、application.yml、SecurityConfig 等）改动时**必须**有 active lock。锁文件**自 2026-05-12 起改为 per-task 单文件**：

```
.agent-locks/<task-slug>.yml      ← 每个任务一个文件，新任务 = 新文件 = 零冲突
.agent-locks.yml                  ← DEPRECATED；仅做 read-only 兼容层
```

acquire/release 仍走相同 CLI（自动写到 per-task 文件）：
```bash
npm run agent:lock-acquire -- --path <path> --scope file|directory --reason "<reason>"
npm run agent:lock-release -- --path <path>
npm run agent:lock-check                # 列所有锁
npm run agent:lock-check:changed        # 仅检查当前改动是否撞锁
```

> **为什么换 per-task 文件**：原 `.agent-locks.yml` 单文件被所有 agent 同时写，每次 rebase 都撞冲突，conditioned everyone to ignore lock warnings。Per-task 文件意味着新任务 → 新文件 → 不打架；janitor 也清得更干净（删文件 vs 删行）。

#### 5.3 Gemini 任务声明（**仅当撞到 `agent/gemini-init` 时有用**）
Gemini 在 `conductor/tracks/` 系统里执行任务，会把 in-progress 任务标 `[~]` 并附 `(@gemini, scope: ...)`。如果 5.1 显示 `agent/gemini-init` 在你的目标路径有未合改动，可以查一眼具体任务上下文：
```bash
grep -h "\[~\]" conductor/tracks/*/plan.md | grep gemini
```

> **重要**：Claude / Codex / Cursor **没有等价的任务声明机制**。撞到这几个 agent 的分支时，**不要假设可以从 plan.md 查到他们的意图** — 直接看 `git log <branch>` 的 commit message，或在 PR 描述里 @ 对方协调。其他 agent 的"意图声明"靠 §6 的 commit message + push 频率自然沉淀。

#### 5.4 撞了的处置
- 等对方 push 完一个原子 commit / PR merge / Gemini 任务标 `[x]`
- 换一个不撞的任务先做
- 在 PR 描述里 @ 对方说明协调结果（"我接着你的 X 改 Y，rebase 你的 PR 后再合"）

#### 5.5 没撞 → 开工
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


<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
specs/002-frontend-permission-migration/plan.md
<!-- SPECKIT END -->
