# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 开发命令

```bash
npm run dev      # 启动开发服务器 (端口: 1314)
npm run build    # 生产环境构建
npm run preview  # 预览构建结果
```

## 端口约定

- Mock 数据前端默认开发端口固定为 `1314`
- 默认访问地址固定为 `http://127.0.0.1:1314` 或 `http://localhost:1314`
- `vite.config.js`、`playwright.config.js`、日常联调、截图和演示统一以 `1314` 为准
- `14173` 这类端口仅允许用于临时排查；排查结束后必须关闭，不作为项目约定，不作为演示入口，不写入测试基线
- 如果本机同时存在 `1314` 和其他临时 Vite 端口，优先保留 `1314`

## 项目架构

西域数智化投标管理平台 — 60天交付项目（2026-04-27 启动，2026-06-25 上线）。
面向西域集团的私有化部署、源码交付项目，当前处于实施准备阶段。

### 技术栈
- **前端框架**: Vue 3 (Composition API)
- **构建工具**: Vite 5
- **UI组件库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **图表**: ECharts
- **样式**: Sass + CSS变量

### 目录结构

```
src/
├── api/
│   └── mock.js           # 所有Mock数据源（无需后端）
├── config/
│   └── ai-prompts.js     # AI功能配置（投标准备、标书编制、团队协作）
├── components/
│   ├── layout/           # 布局组件（MainLayout, Header, Sidebar）
│   ├── charts/           # ECharts图表组件（LineChart, BarChart, PieChart）
│   ├── ai/               # AI相关组件（合规检查、版本管理、协作中心等）
│   └── common/           # 通用组件（TaskBoard, AnimatedNumber等）
├── stores/
│   ├── user.js           # 用户状态（登录、角色切换）
│   ├── project.js        # 项目状态
│   └── bidding.js        # 标讯状态
├── styles/               # 设计系统CSS变量
└── views/                # 页面组件（按功能模块组织）
```

### 路由结构

- `/login` - 登录页
- `/dashboard` - 工作台
- `/bidding` - 标讯中心
- `/project` - 投标项目
- `/knowledge/*` - 知识资产（资质/案例/模板）
- `/resource/*` - 资源管理（费用/账户）
- `/analytics/dashboard` - 数据分析（需admin/manager角色）
- `/ai-center` - AI智能中心

### 状态管理模式

项目使用 Pinia 进行状态管理，stores 位于 `src/stores/`：
- 数据主要从 `src/api/mock.js` 初始化
- 用户状态持久化到 localStorage
- 使用 Composition API 风格的 `defineStore`

### 双模式数据架构

项目支持 Mock 和 API 两种模式，通过 `VITE_API_MODE` 环境变量切换：

- **API 模式**（默认，生产路径）：前端通过 `src/api/modules/` 调用后端 REST API
- **Mock 模式**（演示/开发）：数据来自 `src/api/mock.js`，无需后端

Mock 数据结构包括：users, tenders, projects, qualifications, cases, templates, aiAnalysis, complianceCheck, competitionIntel 等。

### AI功能配置

AI相关功能配置在 `src/config/ai-prompts.js`：
- 三大类别：投标准备、标书编制、团队协作
- 每个功能包含 promptTemplate、formConfig 配置
- 通过 `aiConfigs` 对象管理所有AI功能

### 设计系统

CSS变量定义在 `src/styles/variables.css`，包括：
- 颜色系统（主色、语义色、灰度）
- 间距系统
- 圆角、阴影等

全局样式在 `App.vue` 中导入。

### 路径别名

`@` 别名指向 `src` 目录，在导入时使用：
```js
import Something from '@/components/...'
```

## 常见任务

### 添加新页面
1. 在 `src/views/` 对应模块下创建 Vue 文件
2. 在 `src/router/index.js` 添加路由配置
3. 如需权限控制，添加 `meta.roles`

### 添加新Mock数据
直接在 `src/api/mock.js` 的 `mockData` 对象中添加：
```js
mockData.newEntity = [
  { id: '001', name: '...', ... }
]
```

### 添加AI功能
在 `src/config/ai-prompts.js` 的 `aiConfigs` 中添加新配置，包含：
- `id`, `name`, `icon`, `category`
- `promptTemplate`: role, task, outputFormat
- `formConfig`: 表单配置

### 使用Element Plus组件
所有图标已全局注册，可直接使用：
```vue
<el-icon><Plus /></el-icon>
```

## Wiki 知识库

项目配备了三层 LLM Wiki 知识库，用于结构化管理项目知识：

- **治理规范**: `WIKI.md`（项目根目录）— 定义 Wiki 的维护规则和工作流
- **源文档索引**: `.wiki/INDEX.md` — 对项目 70+ 源文档的分类编目
- **知识页面**: `.wiki/pages/*.md` — LLM 合成的 11 个知识页面

**使用方式**：
1. 回答项目相关问题时，优先查阅 `.wiki/pages/` 中的合成知识
2. 需要细节时，通过页面 frontmatter 的 `sources` 字段追溯到原始文档
3. 新增源文档时，按 `WIKI.md` 中的摄入工作流更新 Wiki

## 项目说明

- 这是一个 60 天交付项目（非 POC），目标是向客户交付可上线的投标管理平台
- 项目支持 Mock 模式（本地演示）和 API 模式（真实后端），默认使用 API 模式
- 后端技术栈：Spring Boot 3.2 + Java 21 + PostgreSQL + Redis
- 部署方式：私有化部署，源码级交付
- 开发服务器默认运行在 http://localhost:1314
- 后端 API 默认运行在 http://localhost:18080
- Mock 演示、联调记录、测试回放默认都使用 `1314` 端口
- 关键文档：`docs/DELIVERY_BLOCKERS_SCHEDULE.md`（交付阻塞项）、`docs/GO_LIVE_CHECKLIST.md`（上线清单）
