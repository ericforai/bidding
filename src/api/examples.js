/**
 * API 使用示例
 * 演示如何在组件和 Store 中使用新的 API 层
 */

// ============================================
// 示例 1: 在 Vue 组件中使用
// ============================================

// <!-- ProjectList.vue -->
/*
<script setup>
import { ref, onMounted } from 'vue'
import { projectsApi } from '@/api'

const projects = ref([])
const loading = ref(false)

// 获取项目列表
const loadProjects = async () => {
  loading.value = true
  try {
    const result = await projectsApi.getList({ status: 'bidding' })
    if (result.success) {
      projects.value = result.data
    }
  } finally {
    loading.value = false
  }
}

// 创建新项目
const createProject = async (projectData) => {
  const result = await projectsApi.create(projectData)
  if (result.success) {
    projects.value.push(result.data)
    ElMessage.success('项目创建成功')
  }
}

onMounted(() => {
  loadProjects()
})
</script>
*/

// ============================================
// 示例 2: 在 Pinia Store 中使用
// ============================================

// stores/user.js (更新后的版本)
/*
import { defineStore } from 'pinia'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token'),
    isAuthenticated: !!localStorage.getItem('token')
  }),

  getters: {
    userInfo: (state) => state.user,
    userRole: (state) => state.user?.role || 'staff',
    isAdmin: (state) => state.user?.role === 'admin'
  },

  actions: {
    async login(username, password) {
      try {
        const result = await authApi.login(username, password)
        if (result.success) {
          this.user = result.data.user
          this.token = result.data.token
          this.isAuthenticated = true
          localStorage.setItem('token', this.token)
          localStorage.setItem('user', JSON.stringify(this.user))
          return { success: true }
        }
        return { success: false, message: result.message }
      } catch (error) {
        return { success: false, message: '登录失败，请稍后重试' }
      }
    },

    async getCurrentUser() {
      const result = await authApi.getCurrentUser()
      if (result.success) {
        this.user = result.data
      }
    },

    logout() {
      this.user = null
      this.token = null
      this.isAuthenticated = false
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
*/

// ============================================
// 示例 3: 各模块 API 调用
// ============================================

export const ApiExamples = {
  // --- 认证模块 ---
  async authExample() {
    const { authApi } = await import('@/api')

    // 登录
    const loginResult = await authApi.login('小王', '123456')
    console.log('登录结果:', loginResult)

    // 获取当前用户
    const userResult = await authApi.getCurrentUser()
    console.log('当前用户:', userResult)

    // 登出
    await authApi.logout()
  },

  // --- 标讯模块 ---
  async tendersExample() {
    const { tendersApi } = await import('@/api')

    // 获取列表
    const listResult = await tendersApi.getList({ status: 'new' })
    console.log('标讯列表:', listResult.data)

    // 获取详情
    const detailResult = await tendersApi.getDetail('B001')
    console.log('标讯详情:', detailResult.data)

    // AI 分析
    const aiResult = await tendersApi.getAIAnalysis('B001')
    console.log('AI 分析:', aiResult.data)
  },

  // --- 项目模块 ---
  async projectsExample() {
    const { projectsApi } = await import('@/api')

    // 获取项目列表
    const projects = await projectsApi.getList()

    // 创建项目
    const newProject = await projectsApi.create({
      name: '新投标项目',
      customer: '某客户公司',
      budget: 500,
      deadline: '2026-12-31',
      industry: '政府'
    })

    // 获取项目任务
    const tasks = await projectsApi.getTasks('P001')
  },

  // --- 知识库模块 ---
  async knowledgeExample() {
    const { knowledgeApi } = await import('@/api')

    // 获取资质列表
    const qualifications = await knowledgeApi.qualifications.getList({ status: 'valid' })

    // 获取案例列表
    const cases = await knowledgeApi.cases.getList({ industry: '政府' })

    // 获取模板列表
    const templates = await knowledgeApi.templates.getList({ category: '技术方案' })
  },

  // --- AI 分析模块 ---
  async aiExample() {
    const { aiApi } = await import('@/api')

    // 评分分析
    const scoreAnalysis = await aiApi.score.getAnalysis('P001')

    // 竞争情报
    const competition = await aiApi.competition.getProjectAnalysis('P001')

    // ROI 分析
    const roi = await aiApi.roi.getAnalysis('P001')

    // 合规检查
    const compliance = await aiApi.compliance.getCheckResult('P001')
  },

  // --- 资源模块 ---
  async resourcesExample() {
    const { resourcesApi } = await import('@/api')

    // 获取平台账户
    const accounts = await resourcesApi.accounts.getList()

    // 获取 BAR 站点
    const sites = await resourcesApi.barSites.getList()

    // 借用 CA 证书
    await resourcesApi.certificates.borrow('UK001', '小王', '某项目')

    // 归还 CA 证书
    await resourcesApi.certificates.return('UK001')
  },

  // --- 协作模块 ---
  async collaborationExample() {
    const { collaborationApi } = await import('@/api')

    // 获取日历事件
    const events = await collaborationApi.calendar.getMonthEvents(2026, 3)

    // 获取紧急事件
    const urgent = await collaborationApi.calendar.getUrgentEvents()

    // 获取讨论线程
    const threads = await collaborationApi.collaboration.getThreads()

    // 添加评论
    await collaborationApi.collaboration.addComment('COLL001', '这是一条新评论')

    // 获取版本历史
    const versions = await collaborationApi.versions.getVersions('DOC001')
  },

  // --- 看板模块 ---
  async dashboardExample() {
    const { dashboardApi } = await import('@/api')

    // 获取统计数据
    const stats = await dashboardApi.dashboard.getStats()

    // 获取趋势数据
    const trend = await dashboardApi.dashboard.getTrend({ period: 'month' })

    // 获取任务列表
    const tasks = await dashboardApi.tasks.getList({ status: 'pending' })

    // 完成任务
    await dashboardApi.tasks.complete('TK001')
  }
}

// ============================================
// 示例 4: 检测当前模式
// ============================================

import { isMockMode } from '@/api/config'

export function showCurrentMode() {
  if (isMockMode()) {
    console.log('📦 当前使用 Mock 数据模式')
  } else {
    console.log('🔌 当前使用真实 API 模式')
  }
}

// ============================================
// 示例 5: 条件使用 Mock 数据
// ============================================

export async function getDataWithFallback() {
  try {
    // 优先尝试真实 API
    const { projectsApi } = await import('@/api')
    const result = await projectsApi.getList()

    if (!isMockMode() && (!result || result.error)) {
      // 如果 API 失败，回退到 Mock 数据
      console.warn('API 调用失败，使用 Mock 数据')
      const { mockData } = await import('@/api')
      return { success: true, data: mockData.projects }
    }

    return result
  } catch (error) {
    console.error('获取数据失败:', error)
    // 最后的 fallback
    const { mockData } = await import('@/api')
    return { success: true, data: mockData.projects }
  }
}

export default ApiExamples
