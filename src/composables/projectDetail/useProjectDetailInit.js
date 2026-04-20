import { onMounted } from 'vue'
import { getDemoAutoTasks, getDemoMobileCard } from '@/api/mock-adapters/frontendDemo.js'

export function useProjectDetailInit(context) {
  const { route, projectStore, knowledgeApi, barStore, approvalApi, projectsApi, isDemoMode } = context

  const loadProjectWorkflowData = async (projectId) => {
    if (!context.project.value || !context.isApiProject.value) return
    const [taskResult, documentResult] = await Promise.all([projectsApi.getTasks(projectId), projectsApi.getDocuments(projectId)])
    context.project.value.tasks = taskResult?.success && Array.isArray(taskResult.data) ? taskResult.data.map((task) => ({ ...task, deliverables: task.deliverables || [], hasDeliverable: Boolean(task.hasDeliverable) })) : []
    context.project.value.documents = documentResult?.success && Array.isArray(documentResult.data) ? documentResult.data : []
  }

  const loadApprovalHistory = async (projectId) => {
    try {
      const result = await approvalApi.getProjectApprovals(projectId)
      context.approvalHistory.value = Array.isArray(result?.data) ? result.data : []
    } catch {
      context.approvalHistory.value = []
    }
  }

  onMounted(async () => {
    context.loading.value = true
    const projectId = route.params.id
    await projectStore.getProjectById(projectId)
    const templateResult = await knowledgeApi.templates.getList()
    context.templates.value = templateResult?.success && Array.isArray(templateResult.data) ? templateResult.data : []
    if (!projectStore.currentProject) projectStore.currentProject = isDemoMode ? context.project.value : null
    await context.loadProjectExpenseAggregation(projectId)
    await loadProjectWorkflowData(projectId)
    if (isDemoMode) {
      context.demoAutoTasks.value = getDemoAutoTasks()
      context.demoMobileCard.value = getDemoMobileCard(projectId)
    }
    await barStore.getSites()
    const currentProject = projectStore.currentProject
    if (currentProject) {
      const matchedSite = barStore.sites.find((site) => site.region === currentProject.region || currentProject.customer?.includes(site.name?.substring(0, 4)))
      if (matchedSite) {
        const result = await barStore.checkSiteCapability(matchedSite.name)
        if (result.found) context.assetCheckResult.value = result
      }
    }
    await loadApprovalHistory(projectId)
    context.loading.value = false
  })

  return { loadProjectWorkflowData, loadApprovalHistory }
}
