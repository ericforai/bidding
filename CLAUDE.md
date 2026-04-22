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
> - `DB_PASSWORD`：PostgreSQL 密码，与容器 `xiyu-bid-rehearsal-postgres` 一致（默认 `XiyuDB!2026`）
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
   当前脚本会给后端注入 `SPRING_PROFILES_ACTIVE=e2e`，给前端注入 `VITE_API_MODE=api` 和 `VITE_API_BASE_URL=http://127.0.0.1:18080`。

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

## Design System
Always read DESIGN.md before making any visual or UI decisions.
All font choices, colors, spacing, and aesthetic direction are defined there.
Do not deviate without explicit user approval.
In QA mode, flag any code that doesn't match DESIGN.md.
