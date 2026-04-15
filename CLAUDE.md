# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 开发命令

```bash
npm run dev      # 启动开发服务器 (端口: 1314)
npm run build    # 生产环境构建
npm run preview  # 预览构建结果
```

## 端口约定

- 前端本地开发服务端口固定为 `1314`
- 默认访问地址固定为 `http://127.0.0.1:1314` 或 `http://localhost:1314`
- `vite.config.js`、`playwright.config.js`、日常联调、截图和演示统一以 `1314` 为准
- `14173` 这类端口仅允许用于临时排查；排查结束后必须关闭，不作为项目约定，不作为演示入口，不写入测试基线
- 如果本机同时存在 `1314` 和其他临时 Vite 端口，优先保留 `1314`

## 验证清单 (Verification Checklist)

所有涉及生产代码（Frontend / Backend）变更的任务，完成后必须作为交付条件经过以下红线扫描：
1. **防止数据回退污染审查**: 运行 `npm run check:front-data-boundaries`，打回任何企图私自使用旧版 mock 兜底的代码。
2. **文档治理与双向同步验证**: 运行 `npm run check:doc-governance`。
3. **架构隐式依赖阻断排查**: 运行 `cd backend && mvn test -Dtest=ArchitectureTest` 确保没有 Controller 직连 Repository 或其他违反六边形架构预设的坏味道。
4. **后端功能与生命周期基准测试**: 运行 `cd backend && mvn clean compile test`。
5. **部署可达性打桩打包**: 至少确保终端命令 `npm run build` 没有抛出任何模块解析异常。

## 核心环境坑点与规避 (Pitfalls)

1. **Spring Security 身份识别错位报错**：在后端 Controller 获取用户上下文时，如果强推 `Authentication.getName()` 到 `Long` 类型 ID 必定抛出 `ClassCastException` 崩溃，因其返回的是业务字符串 `username`。一定要通过 `authService.resolveUserByUsername()` 这个专用工具实现安全身份映射。
2. **前后端接口端口联调错乱**：后端微服务启动默认绑定 **`18080`** 端口，而非某些陈旧 Spring Boot 记忆中的 `8080`。当 VITE 的 API Base 报错连接拒绝时，首先排查是 8080 导致还是 18080 导致。
3. **废弃旧版Mock兜底**：系统已全面移除前端 `mock` 回退机制，专注于真实服务端响应闭环。若前端列表白屏，去查报错或联调真正的后端。
4. **依赖注入越权红线**：禁止在控制层（Controller）隐式使用或注入 `Repository`。如果项目需求促使你这样做，这属于绝对反模式操作，项目内置 ArchUnit CI 随时可能将其识别为构建失败——所有的存储互动必须委托收口给对应 Domain 的 Service。

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
│   └── mock.js           # [已废弃] 遗留演示配置 (不再新加业务数据)
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
- 页面首次加载直接向底层真实 API 根据 Router/Store Hooks 索要数据
- 用户状态持久化到 localStorage
- 使用 Composition API 风格的 `defineStore`

### 数据层架构

项目已全面转入真实产品交付状态，**彻底废弃双模式并丢弃旧有的 Mock 模式**。
前端所有的业务数据和持久化流转，必须通过 `src/api/modules/` 向后端发起 REST API 数据交互请求。

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
- 项目已全面淘汰落后的 Mock 模式，系统强制依赖真实后端接口串联才能进入功能链路
- 后端技术栈：Spring Boot 3.2 + Java 21 + PostgreSQL + Redis
- 部署方式：私有化部署，源码级交付
- 开发服务器默认运行在 http://localhost:1314
- 后端 API 默认运行在 http://localhost:18080
- 本地方案联调记录、测试回放默认都使用 `1314` 开发环境端口
- 关键文档：`docs/DELIVERY_BLOCKERS_SCHEDULE.md`（交付阻塞项）、`docs/GO_LIVE_CHECKLIST.md`（上线清单）
