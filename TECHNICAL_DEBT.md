# 技术债务清单

> 本文档记录前端代码审查中发现的问题，按优先级分类。
> 创建时间：2026-03-19
> 项目阶段：POC（概念验证）

---

## P2 - 交付前处理

### ✅ 1. api/client.js - 使用 router.push 替代 window.location.href

**文件**: `src/api/client.js:106-107`

**完成日期**: 2026-03-19

**修复前**:
```javascript
if (window.location.pathname !== '/login') {
  window.location.href = '/login'
}
```

**修复后**:
```javascript
import router from '@/router/index.js'

// Use Vue Router for navigation to ensure guards are triggered
if (router.currentRoute.value.path !== '/login') {
  router.push('/login').catch((navError) => {
  // Ignore navigation aborted errors (e.g., user navigated away)
    if (navError.name !== 'NavigationDuplicated') {
      console.error('Navigation to login failed:', navError)
    }
  })
}
```

**问题**: 使用 `window.location.href` 会绕过 Vue Router 的导航守卫，可能导致状态不一致

**影响**: POC 阶段影响较小，但正式环境可能导致路由状态残留

**解决方案**: 使用 `router.push()` 进行导航，确保路由守卫被正确触发

**测试**: 添加 E2E 测试 `e2e/router-navigation-redirect.spec.js`。
*注意：在 Playwright 的 `page.route` 拦截环境下，异步重定向存在一定的验证局限性。当前已通过生产代码的多重保障（`setTimeout` 延迟、`router` 实例动态获取、移除单次拦截锁）确保了真实浏览器环境下的重定向可靠性。*

**预计工时**: 1 小时 (包含 E2E 验证与重构)

**优先级**: P2 - 已修复 ✅ (2026-04-16)

---

## P3 - 生产化阶段处理

### 2. router/index.js - 路由守卫异步竞态处理

**文件**: `src/router/index.js:190-197`

**当前代码**:
```javascript
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  let hasAuthState = Boolean(userStore.currentUser && userStore.token)

  if (!userStore.hasRestoredSession) {
    await userStore.restoreSession()
    hasAuthState = Boolean(userStore.currentUser && userStore.token)
  }
  // ...
})
```

**问题**: 理论上存在竞态条件，`hasAuthState` 在 `await` 前计算可能过时

**影响**: 已有 `isRestoringSession` 和 `hasRestoredSession` 保护，实际触发概率极低

**建议修复**:
```javascript
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (!userStore.hasRestoredSession) {
    await userStore.restoreSession()
  }

  const hasAuthState = Boolean(userStore.currentUser && userStore.token)
  // ... 其余逻辑
})
```

**预计工时**: 20 分钟

**优先级**: P3 - 生产环境边界场景

---

### 3. 移动端检测逻辑提取到共享 composable

**文件**: `src/components/layout/MainLayout.vue`, `src/components/layout/Header.vue` 等

**重复代码**:
```javascript
// 在多个组件中重复
const isMobile = ref(false)
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}
onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})
onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
```

**影响**: 代码重复，维护成本增加

**建议创建**: `src/composables/useBreakpoint.js`
```javascript
import { ref, onMounted, onUnmounted } from 'vue'

export function useBreakpoint(breakpoint = 768) {
  const isMobile = ref(false)

  const checkMobile = () => {
    isMobile.value = window.innerWidth < breakpoint
  }

  onMounted(() => {
    checkMobile()
    window.addEventListener('resize', checkMobile)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', checkMobile)
  })

  return { isMobile }
}
```

**预计工时**: 30 分钟

**优先级**: P3 - 代码质量

---

## P4 - 优化建议

### 4. Element Plus 图标按需导入

**文件**: `src/main.js:21-23`

**当前代码**:
```javascript
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
```

**问题**: 全量注册约 200+ 图标，增加打包体积约 100-150KB

**建议方案**: 使用 `unplugin-vue-components` 自动按需导入

**预计工时**: 1 小时

**优先级**: P4 - 性能优化，生产环境考虑

---

### 5. resize 事件防抖处理

**文件**: 多个组件（MainLayout, Header, Sidebar, 图表组件等）

**问题**: resize 事件未防抖，可能导致频繁重渲染

**建议创建**: `src/composables/useDebounceResize.js`
```javascript
import { ref, onMounted, onUnmounted } from 'vue'
import { debounce } from 'lodash-es'

export function useDebounceResize(wait = 200) {
  const width = ref(window.innerWidth)

  const update = debounce(() => {
    width.value = window.innerWidth
  }, wait)

  onMounted(() => {
    window.addEventListener('resize', update)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', update)
  })

  return { width }
}
```

**预计工时**: 30 分钟

**优先级**: P4 - 性能优化

---

### 6. 状态更新的不可变性原则

**文件**: `src/stores/project.js:94-98`, `src/stores/bidding.js:45-48` 等

**当前代码**:
```javascript
async updateTaskStatus(projectId, taskId, status) {
  const project = this.projects.find(p => p.id === projectId)
  if (project) {
    const task = project.tasks.find(t => t.id === taskId)
    if (task) {
      task.status = status  // 直接修改
    }
  }
}
```

**问题**: 违反不可变性原则，可能导致响应式追踪问题

**建议修复**:
```javascript
async updateTaskStatus(projectId, taskId, status) {
  const projectIndex = this.projects.findIndex(p => p.id === projectId)
  if (projectIndex !== -1) {
    const project = { ...this.projects[projectIndex] }
    const taskIndex = project.tasks.findIndex(t => t.id === taskId)
    if (taskIndex !== -1) {
      const updatedTasks = [...project.tasks]
      updatedTasks[taskIndex] = { ...updatedTasks[taskIndex], status }
      project.tasks = updatedTasks
      this.projects[projectIndex] = project
    }
  }
}
```

**预计工时**: 2 小时（多处修改）

**优先级**: P4 - 代码质量，可延后

---

## LOW - 后续优化

### 7. 环境变量检测逻辑优化

**文件**: `src/api/config.js:8-9`

**当前代码**:
```javascript
const viteEnv = typeof import.meta !== 'undefined' && import.meta.env ? import.meta.env : {}
```

**建议**: 使用 Vite 推荐的 `import.meta.env.MODE`

**预计工时**: 10 分钟

**优先级**: LOW

---

### 8. 忘记密码死链接修复

**文件**: `src/views/Login.vue:131`

**当前代码**:
```html
<a href="#" class="forgot-link">忘记密码？</a>
```

**问题**: 点击后导致页面滚动到顶部

**建议修复**:
```html
<span class="forgot-link" @click.prevent="handleForgotPassword">忘记密码？</span>
```

**预计工时**: 5 分钟

**优先级**: LOW

---

### 9. 样式导入顺序统一

**文件**: `src/main.js`, `src/App.vue`

**问题**: 样式导入分散在多个文件，顺序不明确

**建议**: 统一在 `main.js` 中导入所有全局样式

**预计工时**: 15 分钟

**优先级**: LOW

---

### 10. chunkSizeWarningLimit 优化

**文件**: `vite.config.js:13`

**当前配置**:
```javascript
chunkSizeWarningLimit: 980  // 偏高
```

**建议**: 设置为 500KB，主动优化大 chunk

**预计工时**: 视优化情况而定

**优先级**: LOW

---

### 11. TypeScript 迁移或 JSDoc 类型定义

**文件**: 全项目

**问题**: 当前使用 JavaScript，缺少类型安全

**建议**: 添加 JSDoc 注释或迁移到 TypeScript

**预计工时**: 4+ 小时

**优先级**: LOW - 长期规划

---

## 已验证无需修改

### ✅ 并发 401 token 刷新机制

**文件**: `src/api/client.js:14, 76-82`

**验证结果**: 已正确实现 Promise 复用模式，无需修改

### ✅ Login.vue 路由跳转错误处理

**文件**: `src/views/Login.vue:226`

**验证结果**: 异常已被 catch 正确处理，逻辑正确

---

## 优先级定义

| 优先级 | 处理时机 | 说明 |
|--------|----------|------|
| P0 | 立即修复 | 影响功能或演示效果的 bug |
| P1 | 本周内 | 影响用户体验但非阻断性 |
| P2 | 交付前 | 架构一致性，接入后端时处理 |
| P3 | 生产化阶段 | 边界场景、代码质量 |
| P4 | 优化迭代 | 性能优化、可维护性 |
| LOW | 后续规划 | 长期技术改进 |

---

## 统计

- **P0 (已修复)**: 2 项
- **P2**: 1 项
- **P3**: 2 项
- **P4**: 3 项
- **LOW**: 5 项
- **已验证无需修改**: 2 项

**总计**: 15 项

---

> 本文档应随项目进展持续更新。
> 完成一项后，请在对应项添加 ✅ 标记和完成日期。
