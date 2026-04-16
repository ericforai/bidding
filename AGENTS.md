# AGENTS.md - 项目智能体协作口径

本仓库对应“西域数智化投标管理平台”的交付项目。
当前目录名、包名和构件名中仍保留 `xiyu-bid-poc`、`bid-poc` 等历史命名，这些属于遗留标识，不代表项目仍按 POC 方式协作或对外表达。

## 协作口径

- **协作语言**：默认使用中文进行沟通、代码注释、测试说明和变更描述。
- **项目品牌**：对外统一使用“西域数智化投标管理平台”全称；仅在引用仓库路径、包名、脚本名时保留 `xiyu-bid-poc` 等历史标识。
- **开场约定**：AI 代理开启新任务或接收复杂任务时，应先说明当前项目采用“真实 API 单一路径”的交付开发模式；仓库中仍存在少量本地 demo 适配残留，但它们只属于待删除遗留，不是允许的开发、联调、演示或验收路径。随后按 `RULES.md` 中的四阶段流程（plan → tdd → code-review → refactor-clean）展开工作。

## Mock 政策（统一决策）

- **唯一支持路径**：前端、后端、E2E、演示环境均以真实后端 API 为唯一事实源。
- **遗留代码现状**：仓库内仍可见 `frontendDemo` 适配层、`demoPersistence` 等历史遗留；这些内容当前只应被视为清理对象，不允许新增、不允许扩散、不允许恢复为默认路径。
- **执行要求**：任何新功能、Bug 修复、测试回归、截图演示都必须以 `VITE_API_MODE=api` 和真实后端联调为前提。

## 当前项目事实

### 推荐启动方式

**方式一：一键启动（推荐）**
```bash
npm run dev:all
```

说明：根目录 `start.sh` 会尝试拉起后端 `e2e` profile（端口 `18080`）和前端 API 模式开发服务（端口 `1314`）。

**方式二：手动启动**
```bash
# 终端 1：启动后端
cd /Users/user/xiyu/xiyu-bid-poc/backend
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=18080"

# 终端 2：启动前端
cd /Users/user/xiyu/xiyu-bid-poc
VITE_API_MODE=api VITE_API_BASE_URL=http://127.0.0.1:18080 npm run dev -- --host 127.0.0.1 --port 1314
```

### 技术栈

- **前端**：Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router 4 + Axios + ECharts + Sass
- **单元测试**：Vitest
- **端到端测试**：Playwright
- **后端**：Spring Boot 3.2 + Java 21 + Spring Data JPA + PostgreSQL + Redis + Flyway

### 端口约定

| 服务 | 端口 |
|------|------|
| 前端 | 1314 |
| 后端 | 18080 |

### 演示账号

| 用户名 | 角色 | 说明 |
|--------|------|------|
| 小王 | 销售（sales/staff） | 普通业务人员 |
| 张经理 | 经理（manager） | 部门经理 |
| 李总 | 管理员（admin） | 系统管理员 |

## 自证要求

在宣布任务完成前，AI 代理必须主动运行与改动范围匹配的验证命令，并在结论中说明结果：

- **前端改动**：优先运行 `npx vitest run <相关测试文件>`，并至少执行 `npm run check:front-data-boundaries`、`npm run check:doc-governance`、`npm run build`。
- **后端改动**：运行 `mvn test -Dtest=<相关测试类>`；如涉及架构边界，再运行 `mvn test -Dtest=ArchitectureTest`，并把结果作为常规门禁如实汇报。
- **核心链路改动**：运行 `npm run test:e2e`。
- **禁止取巧**：不得通过删除测试、弱化断言、改写验收口径来掩盖问题。

## 当前已知基线

- `npm run check:front-data-boundaries` 当前可通过。
- `npm run check:doc-governance` 当前可通过。
- `npm run build` 当前可通过。
- `backend` 的 `ArchitectureTest` 已修复并恢复全绿；后续若再次失败，应按新增问题处理，不得再写成“已知存量失败”。
