<template>
  <div class="workbench">
    <div class="page-identity">
      <span class="page-kicker">工作台</span>
      <el-tag
        v-if="runtimeModeLabel"
        class="runtime-mode-tag"
        :type="runtimeModeTagType"
        size="small"
      >
        {{ runtimeModeLabel }}
      </el-tag>
    </div>
    <WelcomeBanner
      :role="currentUserRole"
      :title="bannerTitle"
      :subtitle="bannerSubtitle"
      :actions="bannerActions"
      @action-click="handleBannerAction"
    />
    <MetricCards
      :metrics="metrics"
      :loading="metricsLoading"
      :error="metricsError"
      @metric-click="handleMetricClick"
      @retry="reloadMetrics"
    />
    <WorkbenchStaticLayout
      v-if="!dynamicLayout"
      v-model:calendar-date="calendarDate"
      v-model:active-calendar-filter="activeCalendarFilter"
      :calendar-filters="calendarFilters"
      :visible-calendar-events="visibleCalendarEvents"
      :selected-date-events="selectedDateEvents"
      :selected-date-label="selectedDateLabel"
      :month-calendar-summary="monthCalendarSummary"
      :upcoming-calendar-events="upcomingCalendarEvents"
      :get-events-for-date="getEventsForDate"
      :calendar-cell-class="calendarCellClass"
      :get-event-type-tag="getEventTypeTag"
      :calendar-error="calendarError"
      :can-use-quick-start="canUseQuickStart"
      :can-view-tender-list="canViewTenderList"
      :can-view-technical-task="canViewTechnicalTask"
      :can-view-review-list="canViewReviewList"
      :can-view-project-list="canViewProjectList"
      :can-view-team-task="canViewTeamTask"
      :can-view-global-projects="canViewGlobalProjects"
      :hot-tenders="hotTenders"
      :my-technical-tasks="myTechnicalTasks"
      :pending-reviews="pendingReviews"
      :follow-up-customers="followUpCustomers"
      :active-projects="activeProjects"
      :get-progress-color="getProgressColor"
      :get-project-status-type="getProjectStatusType"
      :team-members="teamMembers"
      :current-user-role="currentUserRole"
      :team-performance="teamPerformance"
      :pending-approvals="pendingApprovals"
      :approvals-error="approvalsError"
      :my-processes="myProcesses"
      :format-relative-time="formatRelativeTime"
      :processes-error="processesError"
      :activities="activities"
      :priority-todos="priorityTodos"
      :todos-error="todosError"
      :get-priority-type="getPriorityType"
      :get-priority-label="getPriorityLabel"
      @date-click="handleDateClick"
      @event-date-select="selectCalendarEventDate"
      @event-action="handleCalendarAction"
      @retry-schedule="reloadSchedule"
      @approval-success="handleApprovalSuccess"
      @view-bidding="router.push('/bidding')"
      @tender-click="handleTenderClick"
      @task-change="handleTaskComplete"
      @review="handleReview"
      @view-project="router.push('/project')"
      @project-click="handleProjectClick"
      @approve="handleApprove"
      @reject="handleReject"
      @retry-approvals="loadPendingApprovals"
      @retry-processes="loadMyProcesses"
      @retry-todos="loadTodos"
    />
    <DynamicLayoutRenderer
      v-else
      :layout="dynamicLayout"
      :registry="widgetRegistry"
      :widget-props="widgetProps"
      :widget-listeners="widgetListeners"
    />
    <ApprovalDialog
      v-model:visible="approvalDialogVisible"
      :mode="approvalMode"
      :approval-info="currentApprovalItem"
      @success="handleApprovalSuccess"
    />
  </div>
</template>

<script setup>
import { computed, markRaw, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { dashboardApi, projectsApi, tendersApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useBiddingStore } from '@/stores/bidding'
import { useWorkbenchSchedule } from '@/views/Dashboard/useWorkbenchSchedule.js'
import { useWorkbenchMetrics } from '@/views/Dashboard/useWorkbenchMetrics.js'
import { useWorkbenchTodos } from '@/views/Dashboard/useWorkbenchTodos.js'
import { useWorkbenchApprovals } from '@/views/Dashboard/useWorkbenchApprovals.js'
import { useWorkbenchDerivedLists } from '@/views/Dashboard/useWorkbenchDerivedLists.js'
import { useWorkbenchDynamicWidgets } from '@/views/Dashboard/useWorkbenchDynamicWidgets.js'
import {
  filterProjectsByRole, formatCurrentDate, formatRelativeTime, getBannerActionConfig,
  getBannerSubtitle, getBannerTitle, getPriorityLabel, getPriorityType, getProgressColor,
  getProjectStatusType, hasQuickStartPermission,
} from '@/views/Dashboard/workbench-core.js'
import { normalizeProjectForWorkbench } from '@/views/Dashboard/workbench-utils.js'
import ApprovalDialog from '@/components/common/ApprovalDialog.vue'
import MetricCards from '@/views/Dashboard/components/MetricCards.vue'
import WelcomeBanner from '@/views/Dashboard/components/WelcomeBanner.vue'
import WorkbenchStaticLayout from '@/views/Dashboard/components/WorkbenchStaticLayout.vue'
import DynamicLayoutRenderer from '@/views/Dashboard/components/DynamicLayoutRenderer.vue'
import {
  Briefcase, Calendar, Check, DataAnalysis, Document, Flag, TrendCharts, User,
} from '@element-plus/icons-vue'
import '@/views/Dashboard/styles/workbench-styles.js'

const Icons = markRaw({ Briefcase, Calendar, Check, DataAnalysis, Document, Flag, TrendCharts, User })
const router = useRouter()
const userStore = useUserStore()
const biddingStore = useBiddingStore()

const currentUserRole = computed(() => userStore.currentUser?.role || 'staff')
const currentUserName = computed(() => userStore.currentUser?.name || '用户')
const currentUserId = computed(() => userStore.currentUser?.id || null)
const currentDate = computed(() => formatCurrentDate())
const workbenchProjects = ref([])
const hotTenders = ref([])
const runtimeMode = ref(null)
const dynamicLayout = ref(null)

const {
  pendingApprovals, pendingApprovalsTotalCount, approvalDialogVisible, approvalMode,
  currentApprovalItem, myProcesses, approvalsError, processesError,
  handleApprove, handleReject, handleApprovalSuccess,
  loadPendingApprovals, loadMyProcesses,
} = useWorkbenchApprovals()

const {
  priorityTodos, pendingCount, completedTodoCount, todosError, loadTodos,
  handleTaskComplete,
} = useWorkbenchTodos({ assigneeIdRef: currentUserId, canLoadAlertTodosRef: computed(() => ['admin', 'manager'].includes(currentUserRole.value)), message: ElMessage })

const myProjectCount = computed(() => filterProjectsByRole(workbenchProjects.value, {
  role: currentUserRole.value,
  userName: currentUserName.value,
  limit: Number.POSITIVE_INFINITY,
}).length)

const {
  summaryStats, metricsLoading, metricsError, metrics, loadWorkbenchSummary, handleMetricClick,
} = useWorkbenchMetrics({
  router,
  message: ElMessage,
  currentUserRoleRef: currentUserRole,
  pendingCountRef: pendingCount,
  pendingApprovalsTotalCountRef: pendingApprovalsTotalCount,
  myProjectCountRef: myProjectCount,
  completedTodoCountRef: completedTodoCount,
  icons: Icons,
})

const bannerTitle = computed(() => getBannerTitle(currentUserName.value))
const bannerSubtitle = computed(() => getBannerSubtitle(currentUserRole.value, {
  currentDate: currentDate.value,
  summaryStats: summaryStats.value,
  pendingApprovalsTotalCount: pendingApprovalsTotalCount.value,
  myProjectCount: myProjectCount.value,
  pendingCount: pendingCount.value,
}))
const bannerActions = computed(() => getBannerActionConfig(currentUserRole.value).map(iconizeAction))
const runtimeModeLabel = computed(() => runtimeMode.value?.modeLabel || '')
const runtimeModeTagType = computed(() => (runtimeMode.value?.demoFusionEnabled ? 'warning' : 'success'))
const canUseQuickStart = computed(() => hasQuickStartPermission(userStore.currentUser))

const canViewTenderList = computed(() => userStore.hasPermission('dashboard:view_tender_list') || currentUserRole.value === 'staff')
const canViewTechnicalTask = computed(() => userStore.hasPermission('dashboard:view_technical_task') || currentUserRole.value === 'staff')
const canViewReviewList = computed(() => userStore.hasPermission('dashboard:view_review_list') || ['staff', 'manager'].includes(currentUserRole.value))
const canViewProjectList = computed(() => userStore.hasPermission('dashboard:view_project_list') || currentUserRole.value === 'manager')
const canViewTeamTask = computed(() => userStore.hasPermission('dashboard:view_team_task') || currentUserRole.value === 'manager')
const canViewGlobalProjects = computed(() => userStore.hasPermission('dashboard:view_global_projects'))

const {
  activeProjects, followUpCustomers, teamMembers, myTechnicalTasks,
  pendingReviews, teamPerformance,
} = useWorkbenchDerivedLists({
  workbenchProjects,
  priorityTodos,
  pendingApprovals,
  currentUserRole,
  currentUserName,
})

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
const {
  calendarDate, activeCalendarFilter, selectedDateKey, calendarFilters, visibleCalendarEvents,
  selectedDateEvents, selectedDateLabel, monthCalendarSummary, upcomingCalendarEvents,
  getEventsForDate, calendarCellClass, handleDateClick, getEventTypeTag,
  selectCalendarEventDate, handleCalendarAction, loadScheduleOverview, syncSelectedDate,
  calendarMonthKey, calendarError,
} = useWorkbenchSchedule({
  router,
  assigneeIdRef: currentUserId,
  onEventsLoaded: (events) => biddingStore.setCalendar(events),
})

const { widgetRegistry, widgetProps, widgetListeners } = useWorkbenchDynamicWidgets({
  state: {
    hotTenders,
    myTechnicalTasks,
    pendingReviews,
    followUpCustomers,
    activeProjects,
    currentUserRole,
    teamMembers,
    teamPerformance,
    pendingApprovals,
    approvalsError,
    myProcesses,
    processesError,
    activities,
    priorityTodos,
    todosError,
    calendarDate,
    activeCalendarFilter,
    calendarFilters,
    visibleCalendarEvents,
    selectedDateEvents,
    selectedDateLabel,
    monthCalendarSummary,
    upcomingCalendarEvents,
    calendarError,
  },
  actions: {
    getProgressColor,
    getProjectStatusType,
    formatRelativeTime,
    getEventsForDate,
    calendarCellClass,
    getEventTypeTag,
    viewBidding: () => router.push('/bidding'),
    viewProject: () => router.push('/project'),
    handleTenderClick,
    handleTaskComplete,
    handleReview,
    handleProjectClick,
    handleApprove,
    handleReject,
    loadPendingApprovals,
    loadMyProcesses,
    loadTodos,
    handleApprovalSuccess,
    updateCalendarDate: (value) => { calendarDate.value = value },
    updateActiveCalendarFilter: (value) => { activeCalendarFilter.value = value },
    handleDateClick,
    selectCalendarEventDate,
    handleCalendarAction,
    reloadSchedule,
  },
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

function handleReview(review) {
  ElMessage.info(`打开评审: ${review.title}`)
}

async function reloadMetrics() {
  metricsLoading.value = true
  await loadWorkbenchSummary()
  metricsLoading.value = false
}

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

async function reloadSchedule() {
  await loadScheduleOverview()
  syncSelectedDate()
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

onMounted(async () => {
  metricsLoading.value = true
  await Promise.allSettled([
    loadRuntimeMode(),
    loadDynamicLayout(),
    loadWorkbenchProjects(),
    loadWorkbenchTenders(),
    loadScheduleOverview(), loadTodos(), loadPendingApprovals(), loadMyProcesses(),
    loadWorkbenchSummary(),
  ])
  metricsLoading.value = false
  syncSelectedDate()
})

watch(calendarMonthKey, async (current, previous) => {
  if (!previous || current === previous) return
  await loadScheduleOverview()
  syncSelectedDate()
})
</script>

<script>
export default { name: 'DashboardWorkbench' }
</script>
