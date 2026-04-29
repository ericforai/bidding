<template>
  <div class="content-grid">
    <div class="main-column">
      <WorkCalendar
        :model-value="calendarDate"
        :active-filter="activeCalendarFilter"
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
        @update:model-value="$emit('update:calendarDate', $event)"
        @update:active-filter="$emit('update:activeCalendarFilter', $event)"
        @date-click="$emit('dateClick', $event)"
        @event-date-select="$emit('eventDateSelect', $event)"
        @event-action="$emit('eventAction', $event)"
        @retry="$emit('retrySchedule')"
      />
      <WorkbenchQuickStart v-if="canUseQuickStart" @submitted="$emit('approvalSuccess')" />
      <TenderList
        v-if="canViewTenderList"
        :tenders="hotTenders"
        @view-all="$emit('viewBidding')"
        @tender-click="$emit('tenderClick', $event)"
      />
      <TechnicalTaskList
        v-if="canViewTechnicalTask"
        :tasks="myTechnicalTasks"
        @task-change="$emit('taskChange', $event)"
      />
      <ReviewList
        v-if="canViewReviewList"
        :reviews="pendingReviews"
        @review="$emit('review', $event)"
      />
      <CustomerFollowUpList v-if="canViewTenderList" :customers="followUpCustomers" />
      <ProjectList
        v-if="canViewProjectList"
        title="负责项目"
        :projects="activeProjects"
        :progress-color-resolver="getProgressColor"
        :status-type-resolver="getProjectStatusType"
        @view-all="$emit('viewProject')"
        @project-click="$emit('projectClick', $event)"
        @share-click="$emit('shareClick', $event)"
      />
      <TeamTaskList v-if="canViewTeamTask" :members="teamMembers" />
      <ProjectList
        v-if="canViewGlobalProjects"
        title="全院重点项目"
        :projects="activeProjects"
        :meta-fields="['manager', 'deadline']"
        :progress-color-resolver="getProgressColor"
        :status-type-resolver="getProjectStatusType"
        @view-all="$emit('viewProject')"
        @project-click="$emit('projectClick', $event)"
        @share-click="$emit('shareClick', $event)"
      />
      <ProjectList
        title="进行中项目"
        :projects="activeProjects"
        :progress-color-resolver="getProgressColor"
        :status-type-resolver="getProjectStatusType"
        @view-all="$emit('viewProject')"
        @project-click="$emit('projectClick', $event)"
        @share-click="$emit('shareClick', $event)"
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
            @approve="$emit('approve', $event)"
            @reject="$emit('reject', $event)"
            @retry="$emit('retryApprovals')"
          />
        </div>
      </template>
      <ProcessTimeline
        :processes="myProcesses"
        :time-formatter="formatRelativeTime"
        :error="processesError"
        @retry="$emit('retryProcesses')"
      />
      <ActivityList :activities="activities" />
      <PriorityTodos
        :todos="priorityTodos"
        :error="todosError"
        :priority-type-resolver="getPriorityType"
        :priority-label-resolver="getPriorityLabel"
        @todo-toggle="$emit('taskChange', $event)"
        @retry="$emit('retryTodos')"
      />
    </div>
  </div>
</template>

<script setup>
import ActivityList from '@/views/Dashboard/components/ActivityList.vue'
import ApprovalList from '@/views/Dashboard/components/ApprovalList.vue'
import CustomerFollowUpList from '@/views/Dashboard/components/CustomerFollowUpList.vue'
import PriorityTodos from '@/views/Dashboard/components/PriorityTodos.vue'
import ProcessTimeline from '@/views/Dashboard/components/ProcessTimeline.vue'
import ProjectList from '@/views/Dashboard/components/ProjectList.vue'
import ReviewList from '@/views/Dashboard/components/ReviewList.vue'
import TeamPerformance from '@/views/Dashboard/components/TeamPerformance.vue'
import TeamTaskList from '@/views/Dashboard/components/TeamTaskList.vue'
import TechnicalTaskList from '@/views/Dashboard/components/TechnicalTaskList.vue'
import TenderList from '@/views/Dashboard/components/TenderList.vue'
import WorkbenchQuickStart from '@/views/Dashboard/components/WorkbenchQuickStart.vue'
import WorkCalendar from '@/views/Dashboard/components/WorkCalendar.vue'

defineProps({
  calendarDate: { type: [Date, String], default: null },
  activeCalendarFilter: { type: String, default: '' },
  calendarFilters: { type: Array, default: () => [] },
  visibleCalendarEvents: { type: Array, default: () => [] },
  selectedDateEvents: { type: Array, default: () => [] },
  selectedDateLabel: { type: String, default: '' },
  monthCalendarSummary: { type: Object, default: () => ({}) },
  upcomingCalendarEvents: { type: Array, default: () => [] },
  getEventsForDate: { type: Function, required: true },
  calendarCellClass: { type: Function, required: true },
  getEventTypeTag: { type: Function, required: true },
  calendarError: { type: String, default: '' },
  canUseQuickStart: { type: Boolean, default: false },
  canViewTenderList: { type: Boolean, default: false },
  canViewTechnicalTask: { type: Boolean, default: false },
  canViewReviewList: { type: Boolean, default: false },
  canViewProjectList: { type: Boolean, default: false },
  canViewTeamTask: { type: Boolean, default: false },
  canViewGlobalProjects: { type: Boolean, default: false },
  hotTenders: { type: Array, default: () => [] },
  myTechnicalTasks: { type: Array, default: () => [] },
  pendingReviews: { type: Array, default: () => [] },
  followUpCustomers: { type: Array, default: () => [] },
  activeProjects: { type: Array, default: () => [] },
  getProgressColor: { type: Function, required: true },
  getProjectStatusType: { type: Function, required: true },
  teamMembers: { type: Array, default: () => [] },
  currentUserRole: { type: String, default: 'staff' },
  teamPerformance: { type: Array, default: () => [] },
  pendingApprovals: { type: Array, default: () => [] },
  approvalsError: { type: String, default: '' },
  myProcesses: { type: Array, default: () => [] },
  formatRelativeTime: { type: Function, required: true },
  processesError: { type: String, default: '' },
  activities: { type: Array, default: () => [] },
  priorityTodos: { type: Array, default: () => [] },
  todosError: { type: String, default: '' },
  getPriorityType: { type: Function, required: true },
  getPriorityLabel: { type: Function, required: true },
})

defineEmits([
  'update:calendarDate',
  'update:activeCalendarFilter',
  'dateClick',
  'eventDateSelect',
  'eventAction',
  'retrySchedule',
  'approvalSuccess',
  'viewBidding',
  'tenderClick',
  'taskChange',
  'review',
  'viewProject',
  'projectClick',
  'shareClick',
  'approve',
  'reject',
  'retryApprovals',
  'retryProcesses',
  'retryTodos',
])
</script>
