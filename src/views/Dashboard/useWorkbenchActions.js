import { computed } from 'vue'

export function useWorkbenchActions({
  router, ElMessage, myProcesses, priorityTodos,
  loadWorkbenchSummary, loadScheduleOverview, syncSelectedDate,
  loadWorkbenchProjects, metricsLoading, selectedProjectForCollab, collabDialogVisible,
  Icons
}) {
  const activities = computed(() => {
    const processActivities = myProcesses.value.slice(0, 4).map((process) => ({
      id: `process-${process.id}`,
      type: process.status === 'urgent' ? 'warning' : process.status === 'in-progress' ? 'success' : 'info',
      text: process.title,
      time: process.time || '刚刚',
    }))
    if (processActivities.length > 0) {
      return processActivities
    }
    return priorityTodos.value.slice(0, 4).map((todo) => ({
      id: `todo-${todo.id}`,
      type: todo.done ? 'success' : 'warning',
      text: todo.title,
      time: todo.deadline || '待处理',
    }))
  })

  function iconizeAction(action) {
    return { ...action, icon: Icons[action.icon] || action.icon }
  }

  function handleBannerAction(action) {
    if (action?.target) router.push(action.target)
  }

  function handleTenderClick(tender) {
    if (String(tender.id || '').startsWith('-')) {
      router.push('/bidding')
      return
    }
    router.push(`/bidding/${tender.id}`)
  }

  function handleProjectClick(project) {
    const projectId = String(project?.id || '')
    if (/^\d+$/.test(projectId)) {
      router.push(`/project/${projectId}`)
      return
    }
    router.push({ path: '/project', query: { demoProjectId: projectId } })
  }

  function handleShareClick(project) {
    selectedProjectForCollab.value = project
    collabDialogVisible.value = true
  }

  function handleProjectMemberChanged() {
    loadWorkbenchProjects()
  }

  function handleReview(review) {
    ElMessage.info(`打开评审: ${review.title}`)
  }

  async function reloadMetrics() {
    metricsLoading.value = true
    await loadWorkbenchSummary()
    metricsLoading.value = false
  }

  async function reloadSchedule() {
    await loadScheduleOverview()
    syncSelectedDate()
  }

  return {
    activities,
    iconizeAction,
    handleBannerAction,
    handleTenderClick,
    handleProjectClick,
    handleShareClick,
    handleProjectMemberChanged,
    handleReview,
    reloadMetrics,
    reloadSchedule
  }
}
