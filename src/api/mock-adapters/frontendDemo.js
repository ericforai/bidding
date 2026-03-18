import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'
import { loadDemoState, saveDemoState } from '@/utils/demoPersistence'

export function getDemoUsers() {
  return isMockMode() ? [...(mockData.users || [])] : []
}

export function getDemoTodos() {
  return isMockMode() ? [...(mockData.todos || [])] : []
}

export function getDemoCalendar() {
  return isMockMode() ? [...(mockData.calendar || [])] : []
}

export function getDemoProjects() {
  if (!isMockMode()) return []
  return loadDemoState('projects', mockData.projects || [])
}

export function getDemoProjectById(projectId) {
  if (!isMockMode()) return null
  return getDemoProjects().find((item) => String(item.id) === String(projectId)) || null
}

export function getDemoAutoTasks() {
  return isMockMode() ? [...(mockData.autoTasks || [])] : []
}

export function getDemoMobileCard(projectId) {
  if (!isMockMode()) return null
  return mockData.mobileCard?.[projectId] || mockData.mobileCard?.P001 || null
}

export function getDemoAutomationPanelData() {
  if (!isMockMode()) {
    return {
      rules: [],
      pendingReminders: [],
      executionHistory: [],
    }
  }

  return {
    rules: [
      { id: 1, trigger: '开标前3天', action: '提醒确认投标保证金', enabled: true },
      { id: 2, trigger: '答疑截止前1天', action: '收集内部答疑问题', enabled: true },
      { id: 3, trigger: '资质到期前30天', action: '提醒更新资质', enabled: true },
      { id: 4, trigger: '中标后7天', action: '提醒保证金退还申请', enabled: false },
      { id: 5, trigger: '开标前1天', action: '提醒检查标书盖章', enabled: true },
    ],
    pendingReminders: [
      { id: 1, task: '智慧城市IOC - 投标保证金办理截止', dueTime: '2024-03-15 17:00', urgency: 'high' },
      { id: 2, task: '智慧城市IOC - 技术方案终稿', dueTime: '2024-03-18 12:00', urgency: 'medium' },
      { id: 3, task: 'ISO27001资质即将到期', dueTime: '2024-03-25 00:00', urgency: 'low' },
      { id: 4, task: 'XX项目 - 提交答疑问题', dueTime: '2024-03-16 09:00', urgency: 'high' },
    ],
    executionHistory: [
      {
        id: 1,
        action: '投标保证金提醒',
        detail: '已通过企业微信发送给项目负责人李四',
        timestamp: '2024-03-12 10:30',
        status: 'success',
      },
      {
        id: 2,
        action: '资质更新提醒',
        detail: 'ISO9001资质将于30天后到期',
        timestamp: '2024-03-11 14:20',
        status: 'success',
      },
      {
        id: 3,
        action: '开标提醒',
        detail: 'XX市智慧交通项目今日开标',
        timestamp: '2024-03-10 09:00',
        status: 'warning',
      },
    ],
  }
}

export function getDemoDashboardProjects() {
  return getDemoProjects().map((project) => ({
    ...project,
    result: project.status === 'won' ? 'won' : project.status === 'lost' ? 'lost' : null,
  }))
}

function getCaseState() {
  return isMockMode() ? loadDemoState('knowledge-case-overrides', {}) : {}
}

export function getCaseDemoOverride(caseId) {
  if (!isMockMode()) return null
  return getCaseState()[String(caseId)] || null
}

export function saveCaseDemoPatch(caseId, patch) {
  if (!isMockMode()) return
  const state = getCaseState()
  state[String(caseId)] = {
    ...(state[String(caseId)] || {}),
    ...patch,
  }
  saveDemoState('knowledge-case-overrides', state)
}

export function getTemplateDemoState() {
  if (!isMockMode()) {
    return { patches: {}, copies: [] }
  }

  return loadDemoState('knowledge-template-overrides', {
    patches: {},
    copies: [],
  })
}

export function saveTemplateDemoState(state) {
  if (!isMockMode()) return
  saveDemoState('knowledge-template-overrides', state)
}

function getBarSiteState() {
  return isMockMode() ? loadDemoState('bar-site-overrides', {}) : {}
}

export function getBarSiteDemoOverride(siteId) {
  if (!isMockMode()) return null
  return getBarSiteState()[String(siteId)] || null
}

export function saveBarSiteDemoPatch(siteId, patch) {
  if (!isMockMode()) return
  const state = getBarSiteState()
  const current = state[String(siteId)] || {}
  state[String(siteId)] = {
    ...current,
    ...patch,
    sop: patch.sop ? { ...(current.sop || {}), ...patch.sop } : current.sop,
  }
  saveDemoState('bar-site-overrides', state)
}
