# API 集成指南

## 概述

项目现在支持**双模式切换**：Mock 数据模式 / 真实后端 API 模式

- **Mock 模式**：使用本地 Mock 数据，无需启动后端，适合前端开发和演示
- **API 模式**：调用真实后端接口，需要启动后端服务

## 目录结构

```
src/api/
├── config.js              # API 配置（模式切换、基础URL）
├── client.js              # Axios 客户端封装
├── mock.js                # Mock 数据源（保留原有数据）
├── trendradar.js          # TrendRadar API（已存在）
├── modules/               # 按模块分组的 API 调用
│   ├── auth.js           # 认证模块
│   ├── tenders.js        # 标讯模块
│   ├── projects.js       # 项目模块
│   ├── knowledge.js      # 知识库模块
│   ├── fees.js           # 费用模块
│   ├── ai.js             # AI 分析模块
│   ├── resources.js      # 资源管理模块
│   ├── collaboration.js  # 协作与文档模块
│   └── dashboard.js      # 数据看板与任务模块
└── index.js              # 统一导出
```

## 模式切换

### 方式一：环境变量

创建或修改 `.env` 文件：

```bash
# Mock 模式（默认）
VITE_API_MODE=mock

# 或使用真实 API
VITE_API_MODE=api
VITE_API_BASE_URL=http://localhost:8080
```

### 方式二：命令行参数

```bash
# 使用 Mock 模式启动
npm run dev

# 使用 API 模式启动（需先切换环境变量）
VITE_API_MODE=api npm run dev
```

## 使用示例

### 1. 在组件中使用

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { authApi, projectsApi, tendersApi } from '@/api'

const user = ref(null)
const projects = ref([])

// 登录
const handleLogin = async () => {
  const result = await authApi.login('小王', '123456')
  if (result.success) {
    user.value = result.data.user
    localStorage.setItem('token', result.data.token)
  }
}

// 获取项目列表
const loadProjects = async () => {
  const result = await projectsApi.getList({ status: 'bidding' })
  if (result.success) {
    projects.value = result.data
  }
}

onMounted(() => {
  handleLogin()
  loadProjects()
})
</script>
```

### 2. 在 Pinia Store 中使用

```javascript
// src/stores/user.js
import { defineStore } from 'pinia'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: null
  }),

  actions: {
    async login(username, password) {
      const result = await authApi.login(username, password)
      if (result.success) {
        this.user = result.data.user
        this.token = result.data.token
        localStorage.setItem('token', this.token)
        localStorage.setItem('user', JSON.stringify(this.user))
        return true
      }
      return false
    },

    logout() {
      this.user = null
      this.token = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
```

### 3. 各模块 API 调用示例

```javascript
import {
  authApi,
  tendersApi,
  projectsApi,
  knowledgeApi,
  feesApi,
  aiApi,
  resourcesApi,
  collaborationApi,
  dashboardApi
} from '@/api'

// 认证
await authApi.login('username', 'password')
await authApi.getCurrentUser()
await authApi.logout()

// 标讯
await tendersApi.getList({ status: 'new' })
await tendersApi.getDetail('B001')
await tendersApi.getAIAnalysis('B001')

// 项目
await projectsApi.getList()
await projectsApi.getDetail('P001')
await projectsApi.create({ name: '新项目', customer: '客户A' })
await projectsApi.getTasks('P001')

// 知识库
await knowledgeApi.qualifications.getList()
await knowledgeApi.cases.getList({ industry: '政府' })
await knowledgeApi.templates.getList({ category: '技术方案' })

// 费用
await feesApi.getList({ status: 'pending' })
await feesApi.pay('F001', { method: 'bank' })
await feesApi.getStatistics()

// AI 分析
await aiApi.score.getAnalysis('P001')
await aiApi.competition.getProjectAnalysis('P001')
await aiApi.roi.getAnalysis('P001')
await aiApi.compliance.getCheckResult('P001')

// 资源
await resourcesApi.accounts.getList()
await resourcesApi.barSites.getList()
await resourcesApi.certificates.borrow('UK001', '小王', '某项目')

// 协作
await collaborationApi.calendar.getMonthEvents(2026, 3)
await collaborationApi.collaboration.getThreads()
await collaborationApi.versions.getVersions('DOC001')

// 看板
await dashboardApi.dashboard.getStats()
await dashboardApi.tasks.getList({ status: 'pending' })
```

## API 响应格式

### 成功响应

```javascript
{
  success: true,
  data: { ... },
  message: "操作成功"
}
```

### 错误响应

```javascript
{
  success: false,
  message: "错误信息"
}
```

## 迁移指南

### 从旧 Mock 迁移到新 API 层

**旧方式（直接使用 mockData）：**
```javascript
import { mockData } from '@/api/mock'
const users = mockData.users
```

**新方式（使用 API 层）：**
```javascript
import { mockData } from '@/api'  // 仍可访问原 Mock 数据
const users = mockData.users

// 或使用 API（推荐，支持模式切换）
import { authApi } from '@/api'
const result = await authApi.getCurrentUser()
```

### 批量迁移建议

1. **优先迁移核心功能**：登录、项目列表、标讯列表
2. **保持 Mock 数据完整**：作为 fallback 和演示数据
3. **逐步测试 API 模式**：确保后端接口正常后再切换

## 拦截器说明

### 请求拦截器

自动添加 Token：
```javascript
config.headers.Authorization = `Bearer ${token}`
```

### 响应拦截器

- 401：自动跳转登录页
- 403：显示权限错误
- 404：显示资源不存在
- 500：显示服务器错误
- 网络错误：显示连接失败提示

## 错误处理

```javascript
import { authApi } from '@/api'

try {
  const result = await authApi.login('user', 'pass')
  if (result.success) {
    // 处理成功
  } else {
    // 处理业务错误（result.message）
    ElMessage.error(result.message)
  }
} catch (error) {
  // 网络错误或异常（已由拦截器自动提示）
  console.error('登录失败:', error)
}
```

## 注意事项

1. **Token 管理**：登录后 Token 自动存储到 localStorage，请求自动携带
2. **模式切换**：修改 `.env` 文件后需重启开发服务器
3. **CORS 配置**：后端已配置允许 `http://localhost:5173` 跨域
4. **数据一致性**：Mock 模式下数据不持久化，刷新后恢复初始值
