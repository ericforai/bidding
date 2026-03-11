/**
 * 协作与文档模块 API
 * 支持双模式切换，并在 API 模式下对齐当前后端契约
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

function isNumericId(id) {
  return /^\d+$/.test(String(id))
}

function invalidIdMessage(entityName) {
  return {
    success: false,
    message: `Current backend only supports numeric ${entityName} IDs in API mode`,
  }
}

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

function normalizeThread(item = {}) {
  if (item.title || item.createdAt || item.updatedAt) {
    return {
      id: item.id,
      projectId: item.projectId,
      title: item.title || '未命名讨论',
      type: 'thread',
      status: String(item.status || 'OPEN').toLowerCase(),
      author: item.createdBy ? `用户#${item.createdBy}` : '未知用户',
      avatar: item.createdBy ? String(item.createdBy).slice(-2) : '协',
      content: item.title || '',
      timestamp: formatDateTime(item.updatedAt || item.createdAt),
      resolved: String(item.status || '').toUpperCase() === 'RESOLVED',
      replies: [],
    }
  }

  return {
    id: item.id,
    projectId: item.projectId,
    title: item.content || '协作记录',
    type: item.type || 'comment',
    status: item.resolved ? 'resolved' : 'open',
    author: item.author || '未知用户',
    avatar: item.avatar || String(item.author || '协').slice(0, 1),
    content: item.content || '',
    timestamp: item.time || '',
    resolved: Boolean(item.resolved),
    replies: Array.isArray(item.replies) ? item.replies : [],
    position: item.position || null,
  }
}

function normalizeVersion(item = {}) {
  if (item.versionNumber !== undefined || item.changeSummary !== undefined) {
    return {
      id: item.id,
      version: String(item.versionNumber || ''),
      isCurrent: Boolean(item.isCurrent),
      timestamp: formatDateTime(item.createdAt),
      author: item.createdBy ? `用户#${item.createdBy}` : '未知用户',
      avatar: item.createdBy ? String(item.createdBy).slice(-2) : '版',
      changes: item.changeSummary ? [item.changeSummary] : ['暂无变更摘要'],
      content: item.content || '',
    }
  }

  return {
    id: item.id,
    version: item.version,
    isCurrent: Boolean(item.isCurrent),
    timestamp: item.timestamp || '',
    author: item.author || '未知用户',
    avatar: item.avatar || '版',
    changes: Array.isArray(item.changes) ? item.changes : [],
    content: item.content || '',
  }
}

function normalizeSection(item = {}) {
  return {
    id: item.id,
    chapter: item.name || item.title || '未命名章节',
    owner: item.owner || '',
    assignedBy: item.assignedBy || null,
    status: item.status || (item.content ? 'editing' : 'pending'),
    dueDate: item.dueDate || '',
    locked: Boolean(item.locked),
    lockedBy: item.lockedBy || null,
    lockedAt: item.lockedAt || '',
    children: Array.isArray(item.children) ? item.children.map(normalizeSection) : [],
  }
}

function normalizeVersionCompare(data, versions) {
  if (data?.differences) {
    const versionMap = new Map(versions.map((item) => [String(item.id), item]))
    const oldVersion = versionMap.get(String(data.version1Id)) || null
    const newVersion = versionMap.get(String(data.version2Id)) || null

    return {
      version1: oldVersion,
      version2: newVersion,
      differences: Array.isArray(data.differences) ? data.differences : [],
      content1: data.content1 || '',
      content2: data.content2 || '',
    }
  }

  return data
}

function resolveMockProjectId(projectId) {
  const key = String(projectId || '')
  if (mockData.versionHistory?.[key] || mockData.documentEditor?.[key] || mockData.documentAssembly?.[key]) {
    return key
  }
  return 'P001'
}

function getMockThreads() {
  return (mockData.collaboration || []).map(normalizeThread)
}

function getMockVersions(projectId) {
  const key = resolveMockProjectId(projectId)
  return (mockData.versionHistory?.[key]?.versions || []).map(normalizeVersion)
}

function getMockSections(projectId) {
  const key = resolveMockProjectId(projectId)
  const sections = mockData.documentEditor?.[key]?.sections || []
  return sections.map((section) =>
    normalizeSection({
      id: section.id,
      title: section.name,
      owner: section.owner || '',
      dueDate: section.dueDate || '',
      locked: Boolean(section.locked),
      status: section.status === 'in_progress' ? 'editing' : section.status,
    })
  )
}

export const collaborationApi = {
  async getThreads(params = {}) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: getMockThreads(),
      })
    }

    if (!isNumericId(params.projectId)) {
      return Promise.resolve({ success: true, data: getMockThreads(), message: '使用演示协作数据' })
    }

    const response = await httpClient.get('/api/collaboration/threads', {
      params: { projectId: params.projectId },
    })

    const apiData = Array.isArray(response?.data) ? response.data.map(normalizeThread) : []

    return {
      ...response,
      data: apiData.length > 0 ? apiData : getMockThreads(),
    }
  },

  async getThread(id) {
    if (isMockMode()) {
      const thread = (mockData.collaboration || []).find((item) => String(item.id) === String(id))
      return Promise.resolve({ success: true, data: thread ? normalizeThread(thread) : null })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('thread'))

    const response = await httpClient.get(`/api/collaboration/threads/${id}`)
    return { ...response, data: normalizeThread(response?.data) }
  },

  async createThread(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeThread({
          ...data,
          id: `COLL${Date.now()}`,
          time: new Date().toISOString(),
          author: '当前用户',
          resolved: false,
          replies: [],
        }),
      })
    }
    if (!isNumericId(data?.projectId)) return Promise.resolve(invalidIdMessage('project'))

    return httpClient.post('/api/collaboration/threads', data)
  },

  async addComment(threadId, payload) {
    if (isMockMode()) {
      const content = typeof payload === 'string' ? payload : payload?.content || ''
      return Promise.resolve({
        success: true,
        data: {
          id: `COMM${Date.now()}`,
          threadId,
          author: '当前用户',
          content,
          timestamp: new Date().toISOString(),
        },
      })
    }
    if (!isNumericId(threadId)) return Promise.resolve(invalidIdMessage('thread'))

    return httpClient.post(`/api/collaboration/threads/${threadId}/comments`, payload)
  },

  async getMentions(userId) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: (mockData.collaboration || [])
          .filter((item) => String(item.content || '').includes('@'))
          .map(normalizeThread),
      })
    }
    if (!isNumericId(userId)) return Promise.resolve(invalidIdMessage('user'))

    return httpClient.get('/api/collaboration/mentions', { params: { userId } })
  },
}

export const calendarApi = {
  async getEvents(params) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          let data = [...mockData.calendar]
          if (params?.year && params?.month) {
            const prefix = `${params.year}-${String(params.month).padStart(2, '0')}`
            data = data.filter((event) => event.date.startsWith(prefix))
          }
          resolve({ success: true, data })
        }, 200)
      })
    }
    return httpClient.get('/api/calendar', { params })
  },

  async getMonthEvents(year, month) {
    if (isMockMode()) {
      const prefix = `${year}-${String(month).padStart(2, '0')}`
      const data = mockData.calendar.filter((event) => event.date.startsWith(prefix))
      return Promise.resolve({ success: true, data })
    }
    return httpClient.get(`/api/calendar/month/${year}/${month}`)
  },

  async getUrgentEvents() {
    if (isMockMode()) {
      const data = mockData.calendar.filter((event) => event.urgent)
      return Promise.resolve({ success: true, data })
    }
    return httpClient.get('/api/calendar/urgent')
  },

  async createEvent(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: { ...data, id: `CAL${Date.now()}` },
      })
    }
    return httpClient.post('/api/calendar', data)
  },
}

export const documentVersionsApi = {
  async getVersions(projectId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: getMockVersions(projectId) })
    }
    if (!isNumericId(projectId)) {
      return Promise.resolve({ success: true, data: getMockVersions(projectId), message: '使用演示版本数据' })
    }

    const response = await httpClient.get(`/api/documents/${projectId}/versions`)
    const apiData = Array.isArray(response?.data) ? response.data.map(normalizeVersion) : []
    return {
      ...response,
      data: apiData.length > 0 ? apiData : getMockVersions(projectId),
    }
  },

  async compare(projectId, version1Id, version2Id) {
    if (isMockMode()) {
      const versions = getMockVersions(projectId)
      const version1 = versions.find((item) => String(item.id) === String(version1Id)) || null
      const version2 = versions.find((item) => String(item.id) === String(version2Id)) || null
      return Promise.resolve({
        success: true,
        data: {
          version1,
          version2,
          differences: [
            ...(version1?.changes || []).map((item) => `- ${item}`),
            ...(version2?.changes || []).map((item) => `+ ${item}`),
          ],
          content1: version1?.content || '',
          content2: version2?.content || '',
        },
      })
    }
    if (!isNumericId(projectId) || !isNumericId(version1Id) || !isNumericId(version2Id)) {
      const versions = getMockVersions(projectId)
      const version1 = versions.find((item) => String(item.id) === String(version1Id)) || versions[0] || null
      const version2 = versions.find((item) => String(item.id) === String(version2Id)) || versions[1] || versions[0] || null
      return Promise.resolve({
        success: true,
        data: {
          version1,
          version2,
          differences: [
            ...(version1?.changes || []).map((item) => `- ${item}`),
            ...(version2?.changes || []).map((item) => `+ ${item}`),
          ],
          content1: version1?.content || '',
          content2: version2?.content || '',
        },
        message: '使用演示版本对比数据',
      })
    }

    const versionsResponse = await documentVersionsApi.getVersions(projectId)
    const versionList = Array.isArray(versionsResponse?.data) ? versionsResponse.data : []
    const response = await httpClient.get(`/api/documents/${projectId}/versions/${version1Id}/compare/${version2Id}`)
    return {
      ...response,
      data: normalizeVersionCompare(response?.data, versionList),
    }
  },

  async rollback(projectId, versionId, userId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { projectId, versionId, userId } })
    }
    if (!isNumericId(projectId) || !isNumericId(versionId) || !isNumericId(userId)) {
      return Promise.resolve({ success: true, data: { projectId, versionId, userId }, message: '使用演示版本回滚结果' })
    }

    return httpClient.post(`/api/documents/${projectId}/versions/${versionId}/rollback`, null, {
      params: { userId },
    })
  },

  async createVersion(projectId, data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeVersion({
          id: `v${Date.now()}`,
          version: 'new',
          isCurrent: true,
          timestamp: new Date().toISOString(),
          author: '当前用户',
          avatar: '当',
          changes: [data?.changeSummary || '创建新版本'],
          content: data?.content || '',
        }),
      })
    }
    if (!isNumericId(projectId)) {
      return Promise.resolve({
        success: true,
        data: normalizeVersion({
          id: `v${Date.now()}`,
          version: 'new',
          isCurrent: true,
          timestamp: new Date().toISOString(),
          author: '当前用户',
          avatar: '当',
          changes: [data?.changeSummary || '创建新版本'],
          content: data?.content || '',
        }),
        message: '使用演示版本创建结果',
      })
    }

    return httpClient.post(`/api/documents/${projectId}/versions`, {
      ...data,
      projectId: Number(projectId),
    })
  },
}

export const documentEditorApi = {
  async getStructure(projectId) {
    if (isMockMode()) {
      const editor = mockData.documentEditor?.[resolveMockProjectId(projectId)]
      return Promise.resolve({ success: true, data: editor || null })
    }
    if (!isNumericId(projectId)) {
      const editor = mockData.documentEditor?.[resolveMockProjectId(projectId)]
      return Promise.resolve({ success: true, data: editor || null, message: '使用演示文档结构数据' })
    }

    return httpClient.get(`/api/documents/${projectId}/editor/structure`)
  },

  async updateSection(projectId, sectionId, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { ...data, id: sectionId } })
    }
    if (!isNumericId(projectId) || !isNumericId(sectionId)) return Promise.resolve(invalidIdMessage('project/section'))

    return httpClient.put(`/api/documents/${projectId}/editor/sections/${sectionId}`, data)
  },

  async assignSection(projectId, data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          id: data?.sectionId,
          owner: data?.owner || '',
          dueDate: data?.dueDate || '',
          assignedBy: data?.assignedBy || null,
        },
      })
    }
    if (!isNumericId(projectId) || !isNumericId(data?.sectionId) || !isNumericId(data?.assignedBy)) {
      return Promise.resolve(invalidIdMessage('project/section/user'))
    }

    return httpClient.post(`/api/documents/${projectId}/editor/assignments`, {
      ...data,
      sectionId: Number(data.sectionId),
      assignedBy: Number(data.assignedBy),
    })
  },

  async updateLock(projectId, data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          id: data?.sectionId,
          locked: Boolean(data?.locked),
          lockedBy: data?.userId || null,
        },
      })
    }
    if (!isNumericId(projectId) || !isNumericId(data?.sectionId) || !isNumericId(data?.userId)) {
      return Promise.resolve(invalidIdMessage('project/section/user'))
    }

    return httpClient.post(`/api/documents/${projectId}/editor/locks`, {
      ...data,
      sectionId: Number(data.sectionId),
      userId: Number(data.userId),
    })
  },

  async createReminder(projectId, data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          id: `REM${Date.now()}`,
          projectId,
          sectionId: data?.sectionId,
          recipient: data?.recipient || '',
          message: data?.message || '',
          remindedBy: data?.remindedBy || null,
          remindedAt: new Date().toISOString(),
        },
      })
    }
    if (!isNumericId(projectId) || !isNumericId(data?.sectionId) || !isNumericId(data?.remindedBy)) {
      return Promise.resolve(invalidIdMessage('project/section/user'))
    }

    return httpClient.post(`/api/documents/${projectId}/editor/reminders`, {
      ...data,
      sectionId: Number(data.sectionId),
      remindedBy: Number(data.remindedBy),
    })
  },

  async getTree(projectId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: getMockSections(projectId) })
    }
    if (!isNumericId(projectId)) {
      return Promise.resolve({ success: true, data: getMockSections(projectId), message: '使用演示章节树数据' })
    }

    const response = await httpClient.get(`/api/documents/${projectId}/editor/sections/tree`)
    const apiData = Array.isArray(response?.data) ? response.data.map(normalizeSection) : []
    return {
      ...response,
      data: apiData.length > 0 ? apiData : getMockSections(projectId),
    }
  },
}

export const documentAssemblyApi = {
  async getConfig(projectId) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.documentAssembly?.[projectId] || null,
      })
    }
    if (!isNumericId(projectId)) return Promise.resolve(invalidIdMessage('project'))

    return httpClient.get(`/api/documents/assembly/${projectId}`)
  },

  async assemble(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: { id: `ASM${Date.now()}`, status: 'in_progress', ...data },
      })
    }
    if (!isNumericId(data?.projectId)) return Promise.resolve(invalidIdMessage('project'))

    return httpClient.post(`/api/documents/assembly/${data.projectId}/assemble`, data)
  },
}

export default {
  collaboration: collaborationApi,
  calendar: calendarApi,
  versions: documentVersionsApi,
  editor: documentEditorApi,
  assembly: documentAssemblyApi,
}
