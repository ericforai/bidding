// Input: workbenchApi schedule overview endpoint and router
// Output: workbench schedule state/actions composable for Workbench.vue
// Pos: src/views/Dashboard/ - dashboard feature composables
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { computed, ref } from 'vue'
import { workbenchApi } from '@/api/modules/workbench.js'
import { normalizeCalendarEvent } from '@/views/Dashboard/workbench-utils.js'

const calendarFilters = [
  { label: '全部', value: 'all' },
  { label: '截止', value: 'deadline' },
  { label: '投标', value: 'bid' },
  { label: '开标', value: 'opening' },
  { label: '评审', value: 'review' },
  { label: '高风险', value: 'urgent' },
]

const parseDate = (dateStr) => new Date(`${dateStr}T00:00:00`)

export const formatDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const startOfToday = () => parseDate(formatDateKey(new Date()))

const getDaysUntil = (dateStr) => {
  const oneDay = 24 * 60 * 60 * 1000
  return Math.round((parseDate(dateStr) - startOfToday()) / oneDay)
}

const getEventOwner = (event) => {
  const ownerMap = {
    deadline: '商务专员',
    bid: '投标经理',
    opening: '销售经理',
    review: '技术负责人',
  }
  return ownerMap[event.type] || '项目负责人'
}

const getEventStage = (event) => {
  const stageMap = {
    deadline: '资料收口',
    bid: '递交前确认',
    opening: '现场准备',
    review: '评审跟进',
  }
  return stageMap[event.type] || '节点处理中'
}

const getEventBlocker = (event) => {
  const blockerMap = {
    deadline: '待确认最终材料',
    bid: '待核对报价与盖章',
    opening: '待确认授权与签到',
    review: '待准备答疑材料',
  }
  return blockerMap[event.type] || '待补充信息'
}

const getEventActionLabel = (event) => {
  const actionMap = {
    deadline: '去补材料',
    bid: '去检查递交',
    opening: '看开标准备',
    review: '看评审清单',
  }
  return actionMap[event.type] || '查看详情'
}

const decorateCalendarEvent = (event) => {
  const diffDays = getDaysUntil(event.date)
  const isExpired = diffDays < 0
  const isCritical = event.urgent || diffDays <= 1
  const isWarning = !isCritical && diffDays <= 3

  return {
    ...event,
    diffDays,
    countdownLabel: isExpired ? '已逾期' : diffDays === 0 ? '今天' : `D-${diffDays}`,
    riskLabel: isExpired ? '已逾期' : isCritical ? '高风险' : isWarning ? '需关注' : '常规',
    riskTagType: isExpired ? 'danger' : isCritical ? 'info' : isWarning ? 'warning' : 'info',
    priorityLevel: isExpired || isCritical ? 'priority-critical' : isWarning ? 'priority-warning' : 'priority-normal',
    actionLabel: getEventActionLabel(event),
    dayLabel: event.date.slice(5),
    weekdayLabel: parseDate(event.date).toLocaleDateString('zh-CN', { weekday: 'short' }).replace('周', ''),
    fieldSummary: {
      owner: `负责人 ${getEventOwner(event)}`,
      stage: `阶段 ${getEventStage(event)}`,
      blocker: `阻塞 ${getEventBlocker(event)}`,
    },
  }
}

export function useWorkbenchSchedule({ router, assigneeIdRef, onEventsLoaded } = {}) {
  const calendarDate = ref(new Date())
  const activeCalendarFilter = ref('all')
  const selectedDateKey = ref('')
  const calendarEvents = ref([])

  const normalizedCalendarEvents = computed(() => calendarEvents.value.map(decorateCalendarEvent))

  const visibleCalendarEvents = computed(() => normalizedCalendarEvents.value.filter((event) => {
    if (activeCalendarFilter.value === 'all') return true
    if (activeCalendarFilter.value === 'urgent') return event.urgent || event.priorityLevel === 'priority-critical'
    return event.type === activeCalendarFilter.value
  }))

  const getEventsForDate = (date) => {
    const dateStr = formatDateKey(date)
    return visibleCalendarEvents.value.filter((event) => event.date === dateStr)
  }

  const calendarCellClass = ({ date, viewType }) => {
    if (viewType !== 'month') return ''
    const events = getEventsForDate(date)
    if (events.length === 0) return ''
    if (events.some((event) => event.priorityLevel === 'priority-critical')) return 'calendar-day-urgent'
    if (events.length >= 3) return 'calendar-day-crowded'
    return 'calendar-day-has-event'
  }

  const handleDateClick = (date) => {
    selectedDateKey.value = formatDateKey(date)
    calendarDate.value = date
  }

  const getEventTypeTag = (type) => {
    const map = {
      deadline: { type: 'danger', label: '截止' },
      bid: { type: 'primary', label: '投标' },
      opening: { type: 'success', label: '开标' },
      review: { type: 'warning', label: '评审' },
    }
    return map[type] || { type: 'info', label: '其他' }
  }

  const selectedDateEvents = computed(() =>
    visibleCalendarEvents.value.filter((event) => event.date === selectedDateKey.value)
  )

  const selectedDateLabel = computed(() => {
    const date = parseDate(selectedDateKey.value || formatDateKey(new Date()))
    return date.toLocaleDateString('zh-CN', {
      month: 'long',
      day: 'numeric',
      weekday: 'long',
    })
  })

  const monthCalendarSummary = computed(() => {
    const currentYear = calendarDate.value.getFullYear()
    const currentMonth = calendarDate.value.getMonth()
    const monthEvents = visibleCalendarEvents.value.filter((event) => {
      const eventDate = parseDate(event.date)
      return eventDate.getFullYear() === currentYear && eventDate.getMonth() === currentMonth
    })
    const nextDeadline = monthEvents
      .filter((event) => event.type === 'deadline' && event.diffDays >= 0)
      .sort((a, b) => a.diffDays - b.diffDays)[0]

    return {
      total: monthEvents.length,
      urgent: monthEvents.filter((event) => event.urgent || event.priorityLevel === 'priority-critical').length,
      nextDeadlineLabel: nextDeadline ? nextDeadline.countdownLabel : '暂无',
    }
  })

  const upcomingCalendarEvents = computed(() =>
    visibleCalendarEvents.value
      .filter((event) => event.diffDays >= 0 && event.diffDays <= 7)
      .sort((a, b) => a.diffDays - b.diffDays || a.date.localeCompare(b.date))
  )

  const selectCalendarEventDate = (event) => {
    selectedDateKey.value = event.date
    calendarDate.value = parseDate(event.date)
  }

  const handleCalendarAction = (event) => {
    if (event?.projectId) {
      router.push(`/project/${event.projectId}`)
      return
    }

    router.push({
      path: '/project',
      query: {
        calendarDate: event?.date || '',
        calendarType: event?.eventType || event?.type || '',
      },
    })
  }

  const loadScheduleOverview = async () => {
    const rangeStart = new Date(calendarDate.value)
    rangeStart.setDate(1)
    const rangeEnd = new Date(calendarDate.value)
    rangeEnd.setMonth(rangeEnd.getMonth() + 1, 0)

    const response = await workbenchApi.getScheduleOverview({
      start: rangeStart,
      end: rangeEnd,
      assigneeId: assigneeIdRef?.value || undefined,
    })
    const normalizedEvents = (response?.data?.events || []).map(normalizeCalendarEvent)
    calendarEvents.value = normalizedEvents
    onEventsLoaded?.(normalizedEvents)
    return normalizedEvents
  }

  const syncSelectedDate = () => {
    selectedDateKey.value = formatDateKey(new Date())
    const firstUpcomingEvent = normalizedCalendarEvents.value
      .filter((event) => event.diffDays >= 0)
      .sort((a, b) => a.diffDays - b.diffDays)[0]

    if (firstUpcomingEvent) {
      selectedDateKey.value = firstUpcomingEvent.date
      calendarDate.value = parseDate(firstUpcomingEvent.date)
    }
  }

  const calendarMonthKey = computed(() => {
    const year = calendarDate.value.getFullYear()
    const month = String(calendarDate.value.getMonth() + 1).padStart(2, '0')
    return `${year}-${month}`
  })

  return {
    calendarDate,
    activeCalendarFilter,
    selectedDateKey,
    calendarFilters,
    visibleCalendarEvents,
    selectedDateEvents,
    selectedDateLabel,
    monthCalendarSummary,
    upcomingCalendarEvents,
    getEventsForDate,
    calendarCellClass,
    handleDateClick,
    getEventTypeTag,
    selectCalendarEventDate,
    handleCalendarAction,
    loadScheduleOverview,
    syncSelectedDate,
    calendarMonthKey,
  }
}
