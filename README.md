一旦我所属的文件夹有所变化，请更新我。

# 西域数智化投标管理平台

## 项目简介

**产品背景**：西域数智化投标管理平台是一个面向西域集团业务链路的私有化部署、全生命周期投标协同管理系统。旨在解决企业在投标过程中资产历史沉淀难、跨部门编标协作效率低、合规风险不可控等痛点。

**核心功能**：系统提供从外部商机获取（标讯抓取与AI匹配解读）、投标前中期执行跟踪（立项审批、进度控制、任务分配）、业务知识库管理（企业资质、历史成功案例、各类标书模板），到后端资源支撑保障（保证金等费用审批台账管理）和管理层全景数据分析的完整业务闭环。

当前系统正处于冲刺交付阶段，全面放弃 Mock 模式，专注于真实后端 API 模式的开发与最终生产环境交付。

系统已经具备前后端统一 API 层规范、数据库 Flyway 基线回放能力、关键集成测试验证机制、最小可观测性基线构建，以及规范化上线演练预检架构体系。

## 开发治理

- 前端真实数据源治理：[`docs/FRONTEND_REAL_DATA_GOVERNANCE.md`](docs/FRONTEND_REAL_DATA_GOVERNANCE.md)
- 文档与注释治理规范：[`docs/DOCUMENTATION_GOVERNANCE.md`](docs/DOCUMENTATION_GOVERNANCE.md)
- Wiki 知识库治理：[`WIKI.md`](WIKI.md) — 三层 LLM Wiki 架构（源文档索引 + 合成知识页 + Schema）
- Wiki 知识导航：[`.wiki/pages/_index.md`](.wiki/pages/_index.md) — 11 个知识页面目录

## 技术栈

**前端架构**:
- **核心框架**: Vue 3 (Composition API) + Vite 5
- **UI与样式规范**: Element Plus + SCSS/Sass预处理器 + 原生CSS变量
- **状态管理方案**: Pinia 
- **客户端路由**: Vue Router 4
- **图表数据看板**: ECharts
- **端到端测试**: Playwright

**后端架构与生态**:
- **基础框架**: Spring Boot 3.2 + Java 21 (基于最新LTS)
- **底层数据持久化**: PostgreSQL (结构化存储) + Spring Data JPA
- **缓存与临时会话**: Redis
- **数据库基线版本管理**: Flyway

## 功能模块

### 1. 工作台
- 关键指标统计卡片
- 我的待办事项
- 投标日历
- 进行中项目

### 2. 标讯中心
- 外部标讯获取与入库
- AI智能推荐与评分
- 标讯详情与分析
- 相关案例推荐

### 3. 投标项目
- 项目立项（三步表单）
- 项目详情与进度
- 任务看板（待办/进行中/已完成）
- AI合规检查与质量评分
- 结果录入与闭环

### 4. 知识资产中心
- **资质库**: 企业资质管理，到期预警
- **案例库**: 历史成功案例，支持标签筛选
- **模板库**: 标书模板库，一键使用

### 5. 资源管理
- **费用管理**: 保证金、标书费等费用申请与台账
- **账户管理**: 招标平台账户统一管理

### 6. 数据分析
- 管理层数据看板
- 中标率趋势分析
- 竞争对手分析
- 投入产出分析
- 区域分布分析

### 7. 系统设置
- 用户管理
- 角色权限配置
- 系统参数配置
- 操作日志


## 演示账号

| 用户名 | 角色 | 说明 |
|--------|------|------|
| 小王 | 销售(sales) | 普通销售人员 |
| 张经理 | 经理(manager) | 部门经理 |
| 李总 | 管理员(admin) | 系统管理员 |

## 安装运行

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

- 所有开发与调试必须全链路启动真实后端服务。

### 3. 访问系统

打开浏览器访问: http://127.0.0.1:1314

### 4. 登录

- 使用后端真实用户与鉴权

## E2E 基线

### API 模式 Playwright

- `npm run test:e2e` 会自动准备 API 联调基线
- 若 `127.0.0.1:18080` 和 `127.0.0.1:1314` 已可用，测试直接复用现有环境
- 若本地环境未启动，Playwright 会通过 `scripts/test/start-api-e2e-stack.sh` 自动拉起轻量 `e2e` profile 后端和前端预览
- 测试结束后，仅会自动关闭由本次 Playwright 启动的环境

### 手动控制测试基线

```bash
bash scripts/test/start-api-e2e-stack.sh
npm run test:e2e
bash scripts/test/stop-api-e2e-stack.sh
```

## 项目结构

```
xiyu-bid-poc/
├── src/
│   ├── views/              # 页面组件
│   │   ├── Login.vue       # 登录页
│   │   ├── Dashboard/      # 工作台
│   │   ├── Bidding/        # 标讯管理
│   │   ├── Project/        # 项目管理
│   │   ├── Knowledge/      # 知识资产
│   │   ├── Resource/       # 资源管理
│   │   ├── Analytics/      # 数据分析
│   │   └── System/         # 系统设置
│   ├── components/         # 公共组件
│   │   ├── layout/         # 布局组件
│   │   ├── charts/         # 图表组件
│   │   └── common/         # 通用组件
│   ├── stores/             # Pinia状态管理
│   ├── router/             # 路由配置
│   └── api/                # Mock数据
├── public/                 # 静态资源
├── index.html              # HTML模板
├── vite.config.js          # Vite配置
└── package.json            # 项目配置
```

## 演示流程建议

### 场景：某央企智慧办公项目投标全流程

1. **登录系统** (10s)
   - 输入用户名"小王"登录

2. **查看工作台** (30s)
   - 查看今日概览数据
   - 查看待办任务
   - 查看投标日历

3. **发现标讯** (30s)
   - 进入标讯中心
   - 查看AI推荐的"某央企智慧办公项目"
   - 点击"参与投标"

4. **创建项目** (40s)
   - 填写项目基本信息
   - 点击"同步CRM"模拟数据同步
   - 分解任务给团队成员
   - 提交立项

5. **协同编制** (60s)
   - 查看项目详情
   - 查看任务看板
   - 从模板库选择模板
   - 查看资质库
   - 查看AI检查结果

6. **结果录入** (30s)
   - 录录中标结果
   - 查看费用台账

7. **管理看板** (60s)
   - 切换到管理员账号"李总"
   - 查看数据分析看板
   - 查看各维度图表
   - 点击下钻查看详情

**总时长: 约5分钟**

## 发布与演练

### 预检

```bash
bash scripts/release/preflight.sh
```

### 发布演练

```bash
bash scripts/release/deploy.sh
```

### 本地全流程演练

```bash
bash scripts/release/rehearse-release.sh
```

### 正式签字包

```bash
node scripts/release/build-signoff-packet.mjs
```

### 数据库备份

```bash
bash scripts/release/backup-db.sh
```

### 数据库恢复

```bash
CONFIRM_RESTORE=YES bash scripts/release/restore-db.sh <backup-file>
```

## 上线文档

- `docs/UAT_PLAN.md`
- `docs/UAT_SIGNOFF_TEMPLATE.md`
- `docs/GO_LIVE_CHECKLIST.md`
- `docs/ROLLBACK_RUNBOOK.md`
- `docs/plans/2026-03-10-go-live-execution-plan.md`
- `docs/plans/2026-03-10-go-live-wave-2.md`

## 开发说明

- 全面废弃静态 mock.js，数据交互需经过业务代码中的真实编排与底层关联
- 统一 API 层位于 `src/api/modules/`
- 状态管理使用 Pinia，stores 在 `src/stores/` 目录
- 路由配置在 `src/router/index.js`
- 后端位于 `backend/`，使用 Spring Boot + Flyway + PostgreSQL

## 当前状态

- 已全面切换至真实 API 生产交付模式
- 已建立关键数据库迁移和 PostgreSQL baseline 验证
- 已建立最小 CI 门禁和 Actuator/Prometheus 可观测性基线
- 仍需按上线计划继续完成 UAT、发布演练和剩余真实化收口
