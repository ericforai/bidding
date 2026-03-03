# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 开发命令

```bash
npm run dev      # 启动开发服务器 (端口: 5173)
npm run build    # 生产环境构建
npm run preview  # 预览构建结果
```

## 项目架构

这是一个 Vue 3 + Vite 的 POC 项目，用于演示投标管理平台的核心业务流程。

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

### Mock数据架构

所有数据集中在 `src/api/mock.js`，数据结构包括：
- `users` - 用户信息（小王、张经理、李总）
- `tenders` - 标讯列表
- `projects` - 投标项目
- `qualifications` - 资质库
- `cases` - 案例库
- `templates` - 模板库
- `aiAnalysis` - AI分析结果（按项目ID索引）
- `complianceCheck` - 合规检查结果
- `competitionIntel` - 竞争情报

添加新的Mock数据时，直接在 `mockData` 对象中添加对应字段。

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

## 项目说明

- 这是一个POC项目，使用Mock数据，无需后端服务
- 默认登录用户为"小王"，用户信息会保存在localStorage
- 开发服务器默认运行在 http://localhost:5173
