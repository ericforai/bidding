import { computed } from 'vue'
import { hasQuickStartPermission } from '@/views/Dashboard/workbench-core.js'

export function useWorkbenchPermissions({ userStore, currentUserRole }) {
  const canUseQuickStart = computed(() => hasQuickStartPermission(userStore.currentUser))

  const canViewTenderList = computed(() => 
    userStore.hasPermission('dashboard:view_tender_list') || currentUserRole.value === 'staff'
  )
  const canViewTechnicalTask = computed(() => 
    userStore.hasPermission('dashboard:view_technical_task') || currentUserRole.value === 'staff'
  )
  const canViewReviewList = computed(() => 
    userStore.hasPermission('dashboard:view_review_list') || ['staff', 'manager'].includes(currentUserRole.value)
  )
  const canViewProjectList = computed(() => 
    userStore.hasPermission('dashboard:view_project_list') || currentUserRole.value === 'manager'
  )
  const canViewTeamTask = computed(() => 
    userStore.hasPermission('dashboard:view_team_task') || currentUserRole.value === 'manager'
  )
  const canViewGlobalProjects = computed(() => 
    userStore.hasPermission('dashboard:view_global_projects')
  )

  return {
    canUseQuickStart,
    canViewTenderList,
    canViewTechnicalTask,
    canViewReviewList,
    canViewProjectList,
    canViewTeamTask,
    canViewGlobalProjects,
  }
}
