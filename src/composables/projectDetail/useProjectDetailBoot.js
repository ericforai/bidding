import { onMounted } from 'vue'
import { approvalApi, knowledgeApi } from '@/api'
import { getDemoAutoTasks, getDemoMobileCard } from '@/api/mock-adapters/frontendDemo.js'

export function useProjectDetailBoot(context) {
  const { route, projectStore, barStore, isDemoMode, state, workflow, expenseAggregation, loadProjectWorkflowData, demoAutoTasks, demoMobileCard } = context

  const ensureProjectCollections = () => {
    const currentProject = projectStore.currentProject
    if (!currentProject) {
      return null
    }

    if (!Array.isArray(currentProject.tasks)) {
      currentProject.tasks = []
    }
    if (!Array.isArray(currentProject.documents)) {
      currentProject.documents = []
    }

    return currentProject
  }

  const loadApprovalHistory = async (projectId) => {
    try {
      const result = await approvalApi.getProjectApprovals(projectId)
      state.approvalHistory.value = Array.isArray(result?.data) ? result.data : []
    } catch (error) {
      console.error('加载审批历史失败:', error)
      state.approvalHistory.value = []
    }
  }

  const loadProjectDetailDependencies = async (projectId) => {
    const templatePromise = knowledgeApi.templates.getList()
      .then((templateResult) => {
        workflow.templates.value = templateResult?.success && Array.isArray(templateResult.data) ? templateResult.data : []
      })
      .catch((error) => {
        console.error('加载模板列表失败:', error)
        workflow.templates.value = []
      })

    const expensePromise = expenseAggregation.loadProjectExpenseAggregation(projectId)
      .catch((error) => {
        console.error('加载项目费用聚合失败:', error)
      })

    const workflowPromise = loadProjectWorkflowData(projectId)
      .catch((error) => {
        console.error('加载项目工作流数据失败:', error)
      })

    await Promise.all([templatePromise, expensePromise, workflowPromise])
  }

  onMounted(async () => {
    state.loading.value = true
    const projectId = route.params.id
    try {
      await projectStore.getProjectById(projectId)
      ensureProjectCollections()
      await loadProjectDetailDependencies(projectId)
      ensureProjectCollections()

      if (isDemoMode) {
        demoAutoTasks.value = getDemoAutoTasks()
        demoMobileCard.value = getDemoMobileCard(projectId)
      }

      try {
        await barStore.getSites()
        const currentProject = ensureProjectCollections()
        if (currentProject) {
          const matchedSite = barStore.sites.find((site) => site.region === currentProject.region || currentProject.customer?.includes(site.name?.substring(0, 4)))
          if (matchedSite) {
            const result = await barStore.checkSiteCapability(matchedSite.name)
            if (result?.found) state.assetCheckResult.value = result
          }
        }
      } catch (error) {
        console.error('加载 BAR 资产检查失败:', error)
      }

      await loadApprovalHistory(projectId)
    } finally {
      state.loading.value = false
    }
  })

  return { loadApprovalHistory }
}
