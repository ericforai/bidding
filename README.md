一旦我所属的文件夹有所变化，请更新我。

# 西域数智化投标管理平台

本仓库是“西域数智化投标管理平台”的前后端一体化代码仓库。
目录名、NPM 包名、Maven 构件名中仍保留 `xiyu-bid-poc`、`bid-poc` 等历史命名，这些属于仓库遗留，不代表项目仍按 POC 方式交付。

## 项目背景

西域数智化投标管理平台面向企业投标全生命周期管理场景，目标是把商机获取、项目立项、编标协同、知识复用、费用与资源管理、结果闭环和管理分析统一到一套私有化部署系统中。

当前仓库已经进入真实 API 交付开发阶段：
- **唯一支持路径**：真实后端 API
- **统一决策**：彻底删除双模式，不再把 Mock 作为正常开发路径
- **当前现实**：仓库中仍残留部分 Mock 相关脚本与代码，它们只属于待清理技术债，不应被继续使用或扩散

## 核心功能

### 1. 工作台
- 关键指标统计卡片
- 我的待办事项
- 投标日历
- 进行中项目总览

### 2. 标讯中心
- 外部标讯获取与入库
- AI 匹配与解读
- 标讯详情与分析
- 相关案例推荐

### 3. 投标项目
- 项目立项
- 项目详情与进度跟踪
- 协同任务分配
- AI 合规检查与质量评分
- 投标结果闭环

### 4. 知识资产中心
- 资质库
- 案例库
- 模板库

### 5. 资源管理
- 费用管理
- 招标平台账户管理
- BAR（投标资产台账）相关能力

### 6. 数据分析
- 管理驾驶舱
- 中标率趋势
- 竞争情报分析
- ROI 分析
- 区域与业务分布分析

### 7. 系统设置
- 用户与角色权限
- 系统参数配置
- 预警与审计相关配置

## 技术栈

### 前端
- Vue 3（Composition API）
- Vite 5
- Element Plus
- Pinia
- Vue Router 4
- Axios
- ECharts
- Sass / CSS Variables
- Vitest
- Playwright

### 后端
- Spring Boot 3.2
- Java 21
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway

## 快速开始

### 前置依赖

- Node.js 18+
- npm
- Java 21
- Maven 3.9+
- PostgreSQL
- Redis

### 推荐启动方式

```bash
npm install
npm run dev:all
```

说明：
- `npm run dev:all` 会调用根目录 `start.sh`
- 后端默认启动到 `127.0.0.1:18080`
- 前端默认启动到 `127.0.0.1:1314`
- 前端会以 `VITE_API_MODE=api` 连接真实后端

### 手动启动方式

```bash
# 终端 1：启动后端
cd /Users/user/xiyu/xiyu-bid-poc/backend
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=18080"

# 终端 2：启动前端
cd /Users/user/xiyu/xiyu-bid-poc
VITE_API_MODE=api VITE_API_BASE_URL=http://127.0.0.1:18080 npm run dev -- --host 127.0.0.1 --port 1314
```

### 访问地址

- 前端：<http://127.0.0.1:1314>
- 后端健康检查：<http://127.0.0.1:18080/actuator/health>

## 演示账号

| 用户名 | 角色 | 说明 |
|--------|------|------|
| 小王 | 销售（sales/staff） | 普通业务人员 |
| 张经理 | 经理（manager） | 部门经理 |
| 李总 | 管理员（admin） | 系统管理员 |

## 验证命令

### 前端与文档

```bash
npm run check:front-data-boundaries
npm run check:doc-governance
npm run build
```

### 前端测试

```bash
npm run test:unit
npm run test:e2e
```

### 后端测试

```bash
cd backend
mvn test -Dtest=<相关测试类>
mvn test -Dtest=ArchitectureTest
mvn test
```

## 当前验证基线

截至 2026-04-16：
- `npm run check:front-data-boundaries` 可通过
- `npm run check:doc-governance` 可通过
- `npm run build` 可通过
- `backend` 的 `ArchitectureTest` 已修复并恢复全绿

后端架构测试已修复的历史问题包括：
- `config -> service` 依赖：`E2eDemoDataInitializer` 不再直接依赖 `RoleProfileService`
- `config <-> service` 循环依赖：`RateLimitService` 不再依赖 `ExportConfig`

这意味着 `ArchitectureTest` 已可作为常规后端架构门禁执行；后续任务仍需据实报告验证结果。

## E2E 基线

### API 模式 Playwright

- `npm run test:e2e` 会优先复用已运行的前后端环境
- 若本地 `127.0.0.1:18080` 和 `127.0.0.1:1314` 都可用，测试会直接使用现有环境
- 若环境未启动，Playwright 会调用 `scripts/test/start-api-e2e-stack.sh` 准备 API 联调环境
- 测试结束后，仅会关闭由本次 Playwright 启动的进程

### 手动控制 E2E 基线

```bash
bash scripts/test/start-api-e2e-stack.sh
npm run test:e2e
bash scripts/test/stop-api-e2e-stack.sh
```

## 项目结构

```text
xiyu-bid-poc/
├── src/
│   ├── api/                 # API 层与客户端配置；仍含待删除的历史 mock 遗留
│   ├── components/          # 公共组件
│   ├── config/              # 前端配置
│   ├── router/              # 路由配置
│   ├── stores/              # Pinia 状态管理
│   ├── styles/              # 样式与设计变量
│   ├── utils/               # 工具函数
│   └── views/               # 业务页面（Dashboard/Bidding/Project/Knowledge/Resource/Analytics/System/AI/Document）
├── backend/                 # Spring Boot 后端
├── e2e/                     # Playwright 用例
├── scripts/                 # 校验、测试、发布与辅助脚本
├── docs/                    # 项目治理、交付与实施文档
└── start.sh                 # 一键联调启动脚本
```

## 文档入口

- `AGENTS.md`：协作口径与智能体约定
- `RULES.md`：四阶段流程、核心业务逻辑架构约束、红线与当前基线
- `CLAUDE.md`：执行入口、命令、验证清单与环境坑点
- `docs/FRONTEND_REAL_DATA_GOVERNANCE.md`：前端真实数据治理
- `docs/DOCUMENTATION_GOVERNANCE.md`：文档治理规范
- `WIKI.md`：Wiki 知识库治理说明

## 当前状态

- 已统一决策为“真实 API 单一路径”，不再接受双模式扩张
- 仓库中仍有部分 Mock 遗留代码与命令，后续需要继续清理
- 前端构建与文档治理基线稳定
- 后端架构测试已修复，可作为常规门禁执行
- 仓库命名中的 `POC` 属于历史遗留，后续需要逐步完成命名去 POC 化
