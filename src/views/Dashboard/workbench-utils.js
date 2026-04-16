// Input: hour (optional integer 0-23), apiProject, apiEvent, alert, projects[]
// Output: getTimeGreeting, normalizeProjectForWorkbench, normalizeCalendarEvent,
//         normalizeAlertForTodo, extractCustomersFromProjects
// Pos: src/views/Dashboard/ - Dashboard view utilities
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export function getTimeGreeting(hour) {
  if (hour === undefined || hour === null) hour = new Date().getHours()
  if (hour >= 5 && hour <= 11) return '上午好'
  if (hour >= 12 && hour <= 17) return '下午好'
  return '晚上好'
}

// ---------------------------------------------------------------------------
// Status / progress lookup tables
// ---------------------------------------------------------------------------
const PROJECT_STATUS_MAP = {
  INITIATED: { label: '已立项', progress: 10 },
  PREPARING: { label: '编制中', progress: 35 },
  REVIEWING: { label: '评审中', progress: 60 },
  SEALING:   { label: '封装中', progress: 80 },
  BIDDING:   { label: '投标中', progress: 95 },
  ARCHIVED:  { label: '已归档', progress: 100 },
}

const CALENDAR_TYPE_MAP = {
  DEADLINE:   'deadline',
  SUBMISSION: 'bid',
  REVIEW:     'review',
  MEETING:    'review',
  MILESTONE:  'milestone',
  REMINDER:   'reminder',
}

const ALERT_LEVEL_MAP = {
  CRITICAL: 'urgent',
  HIGH:     'high',
  MEDIUM:   'medium',
  LOW:      'low',
}

// ---------------------------------------------------------------------------
// Priority derivation: days until deadline
// ---------------------------------------------------------------------------
function derivePriority(endDate) {
  if (!endDate) return 'low'
  const now = new Date()
  const deadline = new Date(endDate)
  const diffMs = deadline - now
  const diffDays = diffMs / (1000 * 60 * 60 * 24)
  if (diffDays <= 7) return 'high'
  if (diffDays <= 30) return 'medium'
  return 'low'
}

// ---------------------------------------------------------------------------
// 1. normalizeProjectForWorkbench
// ---------------------------------------------------------------------------
export function normalizeProjectForWorkbench(apiProject) {
  if (!apiProject) {
    return { id: undefined, name: '', status: '', progress: 0, deadline: undefined, manager: '', priority: 'low' }
  }
  const statusInfo = PROJECT_STATUS_MAP[apiProject.status] || { label: '', progress: 0 }
  return {
    id:       apiProject.id,
    name:     apiProject.name || '',
    status:   statusInfo.label,
    progress: statusInfo.progress,
    deadline: apiProject.endDate,
    manager:  apiProject.managerName || '',
    priority: derivePriority(apiProject.endDate),
  }
}

// ---------------------------------------------------------------------------
// 2. normalizeCalendarEvent
// ---------------------------------------------------------------------------
export function normalizeCalendarEvent(apiEvent) {
  if (!apiEvent) {
    return { id: undefined, date: undefined, type: 'reminder', title: '', project: '', urgent: false, description: '' }
  }
  return {
    id:          apiEvent.id,
    date:        apiEvent.eventDate,
    type:        CALENDAR_TYPE_MAP[apiEvent.eventType] || 'reminder',
    title:       apiEvent.title || '',
    project:     apiEvent.title || '',
    urgent:      Boolean(apiEvent.isUrgent),
    description: apiEvent.description || '',
  }
}

// ---------------------------------------------------------------------------
// 3. normalizeAlertForTodo
// ---------------------------------------------------------------------------
export function normalizeAlertForTodo(alert) {
  if (!alert) {
    return { id: 'alert-undefined', title: '', priority: 'low', type: 'warning', done: false, deadline: undefined, sourceType: 'alert' }
  }
  const dateStr = alert.createdAt ? alert.createdAt.slice(0, 10) : undefined
  return {
    id:         `alert-${alert.id}`,
    title:      alert.message || '',
    priority:   ALERT_LEVEL_MAP[alert.level] || 'low',
    type:       'warning',
    done:       Boolean(alert.resolved),
    deadline:   dateStr,
    sourceType: 'alert',
  }
}

// ---------------------------------------------------------------------------
// 4. extractCustomersFromProjects
// ---------------------------------------------------------------------------
export function extractCustomersFromProjects(projects) {
  if (!projects || projects.length === 0) return []

  const customerMap = {}
  for (const project of projects) {
    if (!project.customerManager || !project.customerManagerId) continue
    const key = project.customerManagerId
    if (!customerMap[key]) {
      customerMap[key] = { id: key, name: project.customerManager, statuses: [] }
    }
    customerMap[key].statuses.push(project.status)
  }

  return Object.values(customerMap).map(({ id, name, statuses }) => {
    const hasActive = statuses.some(s => s === 'BIDDING' || s === 'REVIEWING')
    const allArchived = statuses.every(s => s === 'ARCHIVED')
    let status, statusType
    if (hasActive) {
      status = '跟进中'; statusType = 'warning'
    } else if (allArchived) {
      status = '已完成'; statusType = 'success'
    } else {
      status = '新客户'; statusType = 'info'
    }
    return { id, name, company: '', status, statusType, projectCount: statuses.length }
  })
}
