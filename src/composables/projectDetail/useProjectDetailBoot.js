import { onMounted } from 'vue'
import { approvalApi, knowledgeApi } from '@/api'
import { getDemoAutoTasks, getDemoMobileCard } from '@/api/mock-adapters/frontendDemo.js'

export function useProjectDetailBoot(context) {
  const { route, projectStore, barStore, isDemoMode, state, workflow, expenseAggregation, loadProjectWorkflowData, demoAutoTasks, demoMobileCard } = context

  const loadApprovalHistory = async (projectId) => {
    try {
      const result = await approvalApi.getProjectApprovals(projectId)
      state.approvalHistory.value = Array.isArray(result?.data) ? result.data : []
    } catch (error) {
      console.error('加载审批历史失败:', error)
      state.approvalHistory.value = []
    }
  }

  onMounted(async () => {
    state.loading.value = true
    const projectId = route.params.id
    await projectStore.getProjectById(projectId)
    const templateResult = await knowledgeApi.templates.getList()
    workflow.templates.value = templateResult?.success && Array.isArray(templateResult.data) ? templateResult.data : []
    await expenseAggregation.loadProjectExpenseAggregation(projectId)
    await loadProjectWorkflowData(projectId)

    if (isDemoMode) {
      demoAutoTasks.value = getDemoAutoTasks()
      demoMobileCard.value = getDemoMobileCard(projectId)
    }

    await barStore.getSites()
    const currentProject = projectStore.currentProject
    if (currentProject) {
      const matchedSite = barStore.sites.find((site) => site.region === currentProject.region || currentProject.customer?.includes(site.name?.substring(0, 4)))
      if (matchedSite) {
        const result = await barStore.checkSiteCapability(matchedSite.name)
        if (result.found) state.assetCheckResult.value = result
      }
    }

    await loadApprovalHistory(projectId)
    state.loading.value = false
  })

  return { loadApprovalHistory }
}
