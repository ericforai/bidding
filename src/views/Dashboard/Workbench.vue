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
      v-if="!dynamicLayout"
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
      @share-click="handleShareClick"
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
    <ProjectCollaboratorsDialog
      v-model="collabDialogVisible"
      :project="selectedProjectForCollab"
      @changed="handleProjectMemberChanged"
    />
  </div>
</template>

<script setup>
import { computed, markRaw, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useBiddingStore } from '@/stores/bidding'
import { useWorkbenchSchedule } from '@/views/Dashboard/useWorkbenchSchedule.js'
import { useWorkbenchMetrics } from '@/views/Dashboard/useWorkbenchMetrics.js'
import { useWorkbenchTodos } from '@/views/Dashboard/useWorkbenchTodos.js'
import { useWorkbenchApprovals } from '@/views/Dashboard/useWorkbenchApprovals.js'
import { useWorkbenchDerivedLists } from '@/views/Dashboard/useWorkbenchDerivedLists.js'
import { useWorkbenchDynamicWidgets } from '@/views/Dashboard/useWorkbenchDynamicWidgets.js'
import { useWorkbenchInitialData } from '@/views/Dashboard/useWorkbenchInitialData.js'
import { useWorkbenchPermissions } from '@/views/Dashboard/useWorkbenchPermissions.js'
import { useWorkbenchActions } from '@/views/Dashboard/useWorkbenchActions.js'
import {
  formatCurrentDate, formatRelativeTime, getBannerActionConfig,
  getBannerSubtitle, getBannerTitle, getPriorityLabel, getPriorityType, getProgressColor,
  getProjectStatusType,
} from '@/views/Dashboard/workbench-core.js'
import ApprovalDialog from '@/components/common/ApprovalDialog.vue'
import MetricCards from '@/views/Dashboard/components/MetricCards.vue'
import ProjectCollaboratorsDialog from '@/views/Dashboard/components/ProjectCollaboratorsDialog.vue'
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

const {
  workbenchProjects, hotTenders, runtimeMode, dynamicLayout,
  loadWorkbenchProjects, loadWorkbenchTenders, loadRuntimeMode, loadDynamicLayout,
} = useWorkbenchInitialData()

const collabDialogVisible = ref(false)
const selectedProjectForCollab = ref(null)

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

const myProjectCount = computed(() => workbenchProjects.value.length)

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

const {
  canUseQuickStart, canViewTenderList, canViewTechnicalTask, canViewReviewList,
  canViewProjectList, canViewTeamTask, canViewGlobalProjects,
} = useWorkbenchPermissions({ userStore, currentUserRole })

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

const {
  activities, iconizeAction, handleBannerAction, handleTenderClick, handleProjectClick,
  handleShareClick, handleProjectMemberChanged, handleReview, reloadMetrics, reloadSchedule,
} = useWorkbenchActions({
  router, ElMessage, myProcesses, priorityTodos,
  loadWorkbenchSummary, loadScheduleOverview, syncSelectedDate,
  loadWorkbenchProjects, metricsLoading, selectedProjectForCollab, collabDialogVisible,
  Icons,
})

const { widgetRegistry, widgetProps, widgetListeners } = useWorkbenchDynamicWidgets({
  state: {
    hotTenders, myTechnicalTasks, pendingReviews, followUpCustomers, activeProjects,
    currentUserRole, teamMembers, teamPerformance, pendingApprovals, approvalsError,
    myProcesses, processesError, activities, priorityTodos, todosError,
    calendarDate, activeCalendarFilter, calendarFilters, visibleCalendarEvents,
    selectedDateEvents, selectedDateLabel, monthCalendarSummary, upcomingCalendarEvents,
    calendarError, canUseQuickStart, canViewTenderList, canViewTechnicalTask,
    canViewReviewList, canViewProjectList, canViewTeamTask, canViewGlobalProjects,
    metrics, metricsLoading, metricsError,
  },
  actions: {
    getProgressColor, getProjectStatusType, formatRelativeTime, getEventsForDate,
    calendarCellClass, getEventTypeTag, getPriorityType, getPriorityLabel,
    handleMetricClick, reloadMetrics, viewBidding: () => router.push('/bidding'),
    viewProject: () => router.push('/project'), handleTenderClick, handleTaskComplete,
    handleReview, handleProjectClick, handleShareClick, handleApprove, handleReject,
    loadPendingApprovals, loadMyProcesses, loadTodos, handleApprovalSuccess,
    updateCalendarDate: (value) => { calendarDate.value = value },
    updateActiveCalendarFilter: (value) => { activeCalendarFilter.value = value },
    handleDateClick, selectCalendarEventDate, handleCalendarAction, reloadSchedule,
  },
})

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

<style scoped>
.side-summary-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
}
</style>
