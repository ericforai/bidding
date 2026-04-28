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
    <div class="content-grid" v-if="!dynamicLayout">
      <div class="main-column">
        <WorkCalendar
          v-model="calendarDate"
          v-model:active-filter="activeCalendarFilter"
          :filters="calendarFilters"
          :visible-events="visibleCalendarEvents"
          :selected-date-events="selectedDateEvents"
          :selected-date-label="selectedDateLabel"
          :month-summary="monthCalendarSummary"
          :upcoming-events="upcomingCalendarEvents"
          :get-events-for-date="getEventsForDate"
          :calendar-cell-class="calendarCellClass"
          :get-event-type-tag="getEventTypeTag"
          :error="calendarError"
          @date-click="handleDateClick"
          @event-date-select="selectCalendarEventDate"
          @event-action="handleCalendarAction"
          @retry="reloadSchedule"
        />
        <WorkbenchQuickStart v-if="canUseQuickStart" @submitted="handleApprovalSuccess" />
        
        <!-- 核心业务组件：根据权限动态展示 -->
        <TenderList v-if="canViewTenderList" :tenders="hotTenders" @view-all="router.push('/bidding')" @tender-click="handleTenderClick" />
        <TechnicalTaskList v-if="canViewTechnicalTask" :tasks="myTechnicalTasks" @task-change="handleTaskComplete" />
        <ReviewList v-if="canViewReviewList" :reviews="pendingReviews" @review="handleReview" />
        <CustomerFollowUpList v-if="canViewTenderList" :customers="followUpCustomers" />

        <ProjectList
          v-if="canViewProjectList"
          title="负责项目"
          :projects="activeProjects"
          :progress-color-resolver="getProgressColor"
          :status-type-resolver="getProjectStatusType"
          @view-all="router.push('/project')"
          @project-click="handleProjectClick"
        />
        <TeamTaskList v-if="canViewTeamTask" :members="teamMembers" />

        <ProjectList
          v-if="userStore.hasPermission('dashboard:view_global_projects')"
          title="全院重点项目"
          :projects="activeProjects"
          :meta-fields="['manager', 'deadline']"
          :progress-color-resolver="getProgressColor"
          :status-type-resolver="getProjectStatusType"
          @view-all="router.push('/project')"
          @project-click="handleProjectClick"
        />
        <ProjectList
          title="进行中项目"
          :projects="activeProjects"
          :progress-color-resolver="getProgressColor"
          :status-type-resolver="getProjectStatusType"
          @view-all="router.push('/project')"
          @project-click="handleProjectClick"
        />
      </div>
      <div class="side-column">
        <template v-if="currentUserRole === 'admin'">
          <div class="side-summary-grid">
            <TeamPerformance :teams="teamPerformance" />
            <ApprovalList
              :approvals="pendingApprovals"
              :count="pendingApprovals.length"
              :error="approvalsError"
              @approve="handleApprove"
              @reject="handleReject"
              @retry="loadPendingApprovals"
            />
          </div>
        </template>
        <ProcessTimeline
          :processes="myProcesses"
          :time-formatter="formatRelativeTime"
          :error="processesError"
          @retry="loadMyProcesses"
        />
        <ActivityList :activities="activities" />
        <PriorityTodos
          :todos="priorityTodos"
          :error="todosError"
          :priority-type-resolver="getPriorityType"
          :priority-label-resolver="getPriorityLabel"
          @todo-toggle="handleTaskComplete"
          @retry="loadTodos"
        />
      </div>
    </div>
    
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
import {
  filterProjectsByRole, formatCurrentDate, formatRelativeTime, getBannerActionConfig,
  getBannerSubtitle, getBannerTitle, getPriorityLabel, getPriorityType, getProgressColor,
  getProjectStatusType, hasQuickStartPermission,
} from '@/views/Dashboard/workbench-core.js'
import { extractCustomersFromProjects, normalizeProjectForWorkbench } from '@/views/Dashboard/workbench-utils.js'
import ApprovalDialog from '@/components/common/ApprovalDialog.vue'
import ActivityList from '@/views/Dashboard/components/ActivityList.vue'
import ApprovalList from '@/views/Dashboard/components/ApprovalList.vue'
import CustomerFollowUpList from '@/views/Dashboard/components/CustomerFollowUpList.vue'
import MetricCards from '@/views/Dashboard/components/MetricCards.vue'
import PriorityTodos from '@/views/Dashboard/components/PriorityTodos.vue'
import ProcessTimeline from '@/views/Dashboard/components/ProcessTimeline.vue'
import ProjectList from '@/views/Dashboard/components/ProjectList.vue'
import ReviewList from '@/views/Dashboard/components/ReviewList.vue'
import TeamPerformance from '@/views/Dashboard/components/TeamPerformance.vue'
import TeamTaskList from '@/views/Dashboard/components/TeamTaskList.vue'
import TechnicalTaskList from '@/views/Dashboard/components/TechnicalTaskList.vue'
import TenderList from '@/views/Dashboard/components/TenderList.vue'
import WelcomeBanner from '@/views/Dashboard/components/WelcomeBanner.vue'
import WorkbenchQuickStart from '@/views/Dashboard/components/WorkbenchQuickStart.vue'
import WorkCalendar from '@/views/Dashboard/components/WorkCalendar.vue'
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

onMounted(async () => {
  const modeRes = await dashboardApi.getRuntimeMode()
  runtimeMode.value = modeRes.data
  
  try {
    const layoutRes = await dashboardApi.getLayout()
    if (layoutRes?.success && layoutRes.data?.layoutJson && layoutRes.data?.layoutJson !== '[]') {
      dynamicLayout.value = JSON.parse(layoutRes.data.layoutJson)
    }
  } catch (err) {
    console.warn('Failed to load dynamic layout, falling back to static layout', err)
  }
})

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

const activeProjects = computed(() => filterProjectsByRole(workbenchProjects.value, {
  role: currentUserRole.value,
  userName: currentUserName.value,
}))
const followUpCustomers = computed(() => extractCustomersFromProjects(workbenchProjects.value))
const teamMembers = computed(() => {
  const byManager = new Map()
  for (const project of workbenchProjects.value) {
    if (!project?.manager) continue
    const existing = byManager.get(project.manager) || {
      id: project.manager,
      name: project.manager,
      tasks: [],
      workload: '0%',
      workloadLevel: 'low',
    }
    existing.tasks.push({
      id: `${project.id}-task`,
      title: project.name,
      priority: project.priority === 'high' ? 'high' : 'medium',
    })
    byManager.set(project.manager, existing)
  }

  return Array.from(byManager.values()).map((item) => {
    const taskCount = item.tasks.length
    const workload = Math.min(95, 20 + taskCount * 20)
    return {
      ...item,
      workload: `${workload}%`,
      workloadLevel: workload >= 80 ? 'high' : workload >= 50 ? 'medium' : 'low',
    }
  })
})
const myTechnicalTasks = computed(() => priorityTodos.value
  .filter((todo) => todo.sourceType === 'task')
  .slice(0, 6)
  .map((todo) => ({
    id: todo.id,
    title: todo.title,
    project: '项目任务',
    deadline: todo.deadline || '待定',
    priority: todo.priority === 'urgent' || todo.priority === 'high' ? 'high' : 'medium',
    done: todo.done,
  })))
const pendingReviews = computed(() => pendingApprovals.value.slice(0, 6).map((item) => ({
  id: item.id,
  title: item.title,
  author: item.applicantName || '待确认',
  time: item.submitTime || item.time || '刚刚',
})))
const teamPerformance = computed(() => teamMembers.value.map((member) => {
  const projectCount = member.tasks.length
  const wins = member.tasks.filter((task) => task.priority === 'high').length
  const active = Math.max(projectCount - wins, 0)
  return {
    dept: member.name,
    size: Math.max(1, Math.min(12, projectCount * 2)),
    progress: Number.parseInt(member.workload, 10) || 0,
    color: '#3B82F6',
    wins,
    active,
  }
}))

const widgetRegistry = {
  ActivityList,
  ApprovalList,
  CustomerFollowUpList,
  MetricCards,
  PriorityTodos,
  ProcessTimeline,
  ProjectList,
  ReviewList,
  TeamPerformance,
  TeamTaskList,
  TechnicalTaskList,
  TenderList,
  WorkbenchQuickStart,
  WorkCalendar
}

const widgetProps = computed(() => ({
  TenderList: { tenders: hotTenders.value },
  TechnicalTaskList: { tasks: myTechnicalTasks.value },
  ReviewList: { reviews: pendingReviews.value },
  CustomerFollowUpList: { customers: followUpCustomers.value },
  ProjectList: { 
    projects: activeProjects.value, 
    progressColorResolver: getProgressColor, 
    statusTypeResolver: getProjectStatusType,
    metaFields: currentUserRole.value === 'admin' ? ['manager', 'deadline'] : ['deadline', 'manager']
  },
  TeamTaskList: { members: teamMembers.value },
  TeamPerformance: { teams: teamPerformance.value },
  ApprovalList: { approvals: pendingApprovals.value, count: pendingApprovals.value.length, error: approvalsError.value },
  ProcessTimeline: { processes: myProcesses.value, timeFormatter: formatRelativeTime, error: processesError.value },
  ActivityList: { activities: activities.value },
  PriorityTodos: { todos: priorityTodos.value, error: todosError.value, priorityTypeResolver: getPriorityType, priorityLabelResolver: getPriorityLabel },
  WorkbenchQuickStart: { },
  WorkCalendar: { 
    modelValue: calendarDate.value, 
    activeFilter: activeCalendarFilter.value,
    filters: calendarFilters.value,
    visibleEvents: visibleCalendarEvents.value,
    selectedDateEvents: selectedDateEvents.value,
    selectedDateLabel: selectedDateLabel.value,
    monthSummary: monthCalendarSummary.value,
    upcomingEvents: upcomingCalendarEvents.value,
    getEventsForDate,
    calendarCellClass,
    getEventTypeTag,
    error: calendarError.value
  }
}))

const widgetListeners = computed(() => ({
  TenderList: { 'view-all': () => router.push('/bidding'), 'tender-click': handleTenderClick },
  TechnicalTaskList: { 'task-change': handleTaskComplete },
  ReviewList: { 'review': handleReview },
  ProjectList: { 'view-all': () => router.push('/project'), 'project-click': handleProjectClick },
  ApprovalList: { 'approve': handleApprove, 'reject': handleReject, 'retry': loadPendingApprovals },
  ProcessTimeline: { 'retry': loadMyProcesses },
  PriorityTodos: { 'todo-toggle': handleTaskComplete, 'retry': loadTodos },
  WorkbenchQuickStart: { 'submitted': handleApprovalSuccess },
  WorkCalendar: { 
    'update:modelValue': (v) => calendarDate.value = v,
    'update:activeFilter': (v) => activeCalendarFilter.value = v,
    'date-click': handleDateClick,
    'event-date-select': selectCalendarEventDate,
    'event-action': handleCalendarAction,
    'retry': reloadSchedule
  }
}))
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

onMounted(async () => {
  metricsLoading.value = true
  await Promise.allSettled([
    loadRuntimeMode(),
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
