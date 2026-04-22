<template>
  <div class="workbench">
    <div class="page-identity"><span class="page-kicker">工作台</span></div>
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
    <div class="content-grid">
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
        <template v-if="currentUserName === '小王'">
          <QuickActions :actions="mappedQuickActions" @action-click="handleQuickAction" />
          <TenderList :tenders="hotTenders" @view-all="router.push('/bidding')" @tender-click="handleTenderClick" />
          <CustomerFollowUpList :customers="followUpCustomers" />
        </template>
        <template v-if="currentUserName === '张经理'">
          <ProjectList
            title="我的项目"
            :projects="activeProjects"
            :progress-color-resolver="getProgressColor"
            :status-type-resolver="getProjectStatusType"
            @view-all="router.push('/project')"
            @project-click="handleProjectClick"
          />
          <TeamTaskList :members="teamMembers" />
        </template>
        <template v-if="currentUserName === '李工'">
          <TechnicalTaskList :tasks="myTechnicalTasks" @task-change="handleTaskComplete" />
          <ReviewList :reviews="pendingReviews" @review="handleReview" />
        </template>
        <ProjectList
          v-if="currentUserRole === 'admin'"
          title="重点项目"
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
    <ApprovalDialog
      v-model:visible="approvalDialogVisible"
      :mode="approvalMode"
      :approval-info="currentApprovalItem"
      @success="handleApprovalSuccess"
    />
    <SupportRequestDialog
      v-model="supportRequestDialogVisible"
      v-model:form="supportRequestForm"
      :projects="supportRequestProjects"
      :projects-error="supportProjectsError"
      :submitting="supportRequestSubmitting"
      @submit="submitSupportRequest"
      @retry-projects="reloadSupportProjects"
    />
  </div>
</template>

<script setup>
import { computed, markRaw, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useBiddingStore } from '@/stores/bidding'
import { useWorkbenchSchedule } from '@/views/Dashboard/useWorkbenchSchedule.js'
import { useWorkbenchMetrics } from '@/views/Dashboard/useWorkbenchMetrics.js'
import { useWorkbenchTodos } from '@/views/Dashboard/useWorkbenchTodos.js'
import { useWorkbenchApprovals } from '@/views/Dashboard/useWorkbenchApprovals.js'
import { useSupportRequest } from '@/views/Dashboard/useSupportRequest.js'
import {
  filterProjectsByRole, formatCurrentDate, formatRelativeTime, getBannerActionConfig,
  getBannerSubtitle, getBannerTitle, getPriorityLabel, getPriorityType, getProgressColor,
  getProjectStatusType,
} from '@/views/Dashboard/workbench-core.js'
import {
  activities, demoProjects, followUpCustomers, hotTenders, myTechnicalTasks,
  pendingReviews, quickActions, teamMembers, teamPerformance,
} from '@/views/Dashboard/workbench-demo-data.js'
import ApprovalDialog from '@/components/common/ApprovalDialog.vue'
import ActivityList from '@/views/Dashboard/components/ActivityList.vue'
import ApprovalList from '@/views/Dashboard/components/ApprovalList.vue'
import CustomerFollowUpList from '@/views/Dashboard/components/CustomerFollowUpList.vue'
import MetricCards from '@/views/Dashboard/components/MetricCards.vue'
import PriorityTodos from '@/views/Dashboard/components/PriorityTodos.vue'
import ProcessTimeline from '@/views/Dashboard/components/ProcessTimeline.vue'
import ProjectList from '@/views/Dashboard/components/ProjectList.vue'
import QuickActions from '@/views/Dashboard/components/QuickActions.vue'
import ReviewList from '@/views/Dashboard/components/ReviewList.vue'
import SupportRequestDialog from '@/views/Dashboard/components/SupportRequestDialog.vue'
import TeamPerformance from '@/views/Dashboard/components/TeamPerformance.vue'
import TeamTaskList from '@/views/Dashboard/components/TeamTaskList.vue'
import TechnicalTaskList from '@/views/Dashboard/components/TechnicalTaskList.vue'
import TenderList from '@/views/Dashboard/components/TenderList.vue'
import WelcomeBanner from '@/views/Dashboard/components/WelcomeBanner.vue'
import WorkCalendar from '@/views/Dashboard/components/WorkCalendar.vue'
import {
  Briefcase, Calendar, Check, DataAnalysis, Document, Flag, FolderOpened, TrendCharts, User, Wallet,
} from '@element-plus/icons-vue'
import '@/views/Dashboard/styles/workbench-styles.js'

const Icons = markRaw({ Briefcase, Calendar, Check, DataAnalysis, Document, Flag, FolderOpened, TrendCharts, User, Wallet })
const router = useRouter()
const userStore = useUserStore()
const biddingStore = useBiddingStore()

const currentUserRole = computed(() => userStore.currentUser?.role || 'staff')
const currentUserName = computed(() => userStore.currentUser?.name || '用户')
const currentUserId = computed(() => userStore.currentUser?.id || null)
const currentDate = computed(() => formatCurrentDate())

const {
  pendingApprovals, pendingApprovalsTotalCount, approvalDialogVisible, approvalMode,
  currentApprovalItem, myProcesses, approvalsError, processesError,
  handleApprove, handleReject, handleApprovalSuccess,
  loadPendingApprovals, loadMyProcesses,
} = useWorkbenchApprovals()

const {
  supportRequestDialogVisible, supportRequestSubmitting, supportRequestProjects,
  supportRequestForm, supportProjectsError, myProjectCount, loadSupportRequestProjects, resetSupportRequestForm,
  openSupportRequestDialog, submitSupportRequest,
} = useSupportRequest({ message: ElMessage, onSubmitted: handleApprovalSuccess })

const {
  priorityTodos, pendingCount, completedTodoCount, todosError, loadTodos,
  handleTaskComplete,
} = useWorkbenchTodos({ assigneeIdRef: currentUserId, message: ElMessage })

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
const mappedQuickActions = computed(() => quickActions.map(iconizeAction))
const activeProjects = computed(() => filterProjectsByRole(demoProjects, {
  role: currentUserRole.value,
  userName: currentUserName.value,
}))

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

function handleQuickAction(action) {
  if (action.key === 'support') openSupportRequestDialog()
  if (action.key === 'borrow') router.push('/resource/contract-borrow')
  if (action.key === 'expense') router.push('/resource/expense')
}

function handleTenderClick(tender) {
  router.push(`/bidding/${tender.id}`)
}

function handleProjectClick(project) {
  router.push(`/project/${project.id}`)
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

async function reloadSupportProjects() {
  await loadSupportRequestProjects()
  resetSupportRequestForm()
}

onMounted(async () => {
  metricsLoading.value = true
  await Promise.allSettled([
    loadScheduleOverview(), loadTodos(), loadPendingApprovals(), loadMyProcesses(),
    loadSupportRequestProjects(), loadWorkbenchSummary(),
  ])
  metricsLoading.value = false
  resetSupportRequestForm()
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
