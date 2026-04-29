import { ref } from 'vue'
import { dashboardApi, projectsApi, tendersApi } from '@/api'
import { normalizeProjectForWorkbench } from '@/views/Dashboard/workbench-utils.js'

export function useWorkbenchInitialData() {
  const workbenchProjects = ref([])
  const hotTenders = ref([])
  const runtimeMode = ref(null)
  const dynamicLayout = ref(null)

  async function loadWorkbenchProjects() {
    try {
      const response = await projectsApi.getList()
      workbenchProjects.value = Array.isArray(response?.data)
        ? response.data.map(normalizeProjectForWorkbench)
        : []
    } catch {
      workbenchProjects.value = []
    }
  }

  async function loadWorkbenchTenders() {
    try {
      const response = await tendersApi.getList()
      const tenders = Array.isArray(response?.data) ? response.data : []
      hotTenders.value = tenders.slice(0, 6).map((item) => {
        const score = Number(item.aiScore || 0)
        const probability = score >= 85 ? 'high' : 'medium'
        return {
          id: item.id,
          title: item.title || '未命名标讯',
          budget: Number(item.budget || 0),
          region: item.region || '-',
          aiScore: score,
          scoreLevel: score >= 85 ? 'high' : score >= 70 ? 'medium' : 'low',
          probability,
          probibilityText: probability === 'high' ? '高概率' : '中等概率',
        }
      })
    } catch {
      hotTenders.value = []
    }
  }

  async function loadRuntimeMode() {
    try {
      const response = await dashboardApi.getRuntimeMode()
      runtimeMode.value = response?.success ? response.data : null
    } catch {
      runtimeMode.value = null
    }
  }

  async function loadDynamicLayout() {
    try {
      const response = await dashboardApi.getLayout()
      const layoutJson = response?.data?.layoutJson
      dynamicLayout.value = response?.success && layoutJson && layoutJson !== '[]'
        ? JSON.parse(layoutJson)
        : null
    } catch (error) {
      console.warn('Failed to load dynamic layout, falling back to static layout', error)
      dynamicLayout.value = null
    }
  }

  return {
    workbenchProjects,
    hotTenders,
    runtimeMode,
    dynamicLayout,
    loadWorkbenchProjects,
    loadWorkbenchTenders,
    loadRuntimeMode,
    loadDynamicLayout,
  }
}
