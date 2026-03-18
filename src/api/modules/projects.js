/**
 * 项目模块 API
 * 支持双模式切换
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'
import { loadDemoState, saveDemoState } from '@/utils/demoPersistence'

function matchesProjectStatus(projectStatus, filterStatus) {
  return String(projectStatus || '').toLowerCase() === String(filterStatus || '').toLowerCase()
}

function applyProjectFilters(projects, params = {}) {
  return projects.filter((project) => {
    if (params.status && !matchesProjectStatus(project.status, params.status)) {
      return false
    }

    if (params.managerId && String(project.managerId) !== String(params.managerId)) {
      return false
    }

    if (params.tenderId && String(project.tenderId) !== String(params.tenderId)) {
      return false
    }

    if (params.name) {
      const keyword = String(params.name).trim().toLowerCase()
      if (!String(project.name || '').toLowerCase().includes(keyword)) {
        return false
      }
    }

    return true
  })
}

function isNumericId(id) {
  return /^\d+$/.test(String(id))
}

function apiModeFailure(entityName) {
  return {
    success: false,
    message: `Current backend only supports numeric ${entityName} IDs in API mode`
  }
}

function getMockProjects(params = {}) {
  if (isMockMode()) {
    mockData.projects = loadDemoState('projects', mockData.projects)
  }
  return applyProjectFilters([...mockData.projects], params)
}

function getMockProject(id) {
  if (isMockMode()) {
    mockData.projects = loadDemoState('projects', mockData.projects)
  }
  return mockData.projects.find((project) => String(project.id) === String(id)) || null
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function buildMockScoreDrafts(projectId, fileName = '评分标准示例.docx') {
  return [
    {
      id: `SD_${projectId}_001`,
      projectId,
      sourceFileName: fileName,
      category: 'technical',
      scoreItemTitle: '项目经理资质',
      scoreRuleText: '提供一级建造师证书得3分',
      scoreValueText: '3分',
      taskAction: '准备',
      generatedTaskTitle: '准备项目经理资质（3分）',
      generatedTaskDescription: '评分目标：项目经理资质\n分值规则：3分\n评分原文：提供一级建造师证书得3分\n执行要求：请准备对应证书及支撑材料。\n完成标准：材料齐全、可直接支撑该项得分判断。',
      suggestedDeliverables: ['证书扫描件', '有效期说明'],
      assigneeId: null,
      assigneeName: '',
      dueDate: '',
      status: 'DRAFT',
      skipReason: '',
      sourcePage: null,
      sourceTableIndex: 0,
      sourceRowIndex: 1,
      generatedTaskId: null,
    },
    {
      id: `SD_${projectId}_002`,
      projectId,
      sourceFileName: fileName,
      category: 'business',
      scoreItemTitle: '同类项目业绩',
      scoreRuleText: '每提供1个同类项目业绩得2分，最高6分',
      scoreValueText: '最高6分',
      taskAction: '整理',
      generatedTaskTitle: '整理同类项目业绩（最高6分）',
      generatedTaskDescription: '评分目标：同类项目业绩\n分值规则：最高6分\n评分原文：每提供1个同类项目业绩得2分，最高6分\n执行要求：请整理业绩合同和验收证明。\n完成标准：材料齐全、可直接支撑该项得分判断。',
      suggestedDeliverables: ['合同关键页', '验收证明', '项目简介'],
      assigneeId: null,
      assigneeName: '',
      dueDate: '',
      status: 'DRAFT',
      skipReason: '',
      sourcePage: null,
      sourceTableIndex: 0,
      sourceRowIndex: 2,
      generatedTaskId: null,
    },
    {
      id: `SD_${projectId}_003`,
      projectId,
      sourceFileName: fileName,
      category: 'price',
      scoreItemTitle: '报价得分',
      scoreRuleText: '按报价偏差率公式计算得分',
      scoreValueText: '10分',
      taskAction: '复核',
      generatedTaskTitle: '复核报价得分（10分）',
      generatedTaskDescription: '评分目标：报价得分\n分值规则：10分\n评分原文：按报价偏差率公式计算得分\n执行要求：请复核报价表、测算依据与公式说明。\n完成标准：材料齐全、可直接支撑该项得分判断。',
      suggestedDeliverables: ['报价表', '测算依据', '公式说明'],
      assigneeId: null,
      assigneeName: '',
      dueDate: '',
      status: 'DRAFT',
      skipReason: '',
      sourcePage: null,
      sourceTableIndex: 0,
      sourceRowIndex: 3,
      generatedTaskId: null,
    }
  ]
}

function ensureMockScoreDrafts(projectId, fileName) {
  if (!mockData.projectScoreDrafts[projectId]) {
    mockData.projectScoreDrafts[projectId] = buildMockScoreDrafts(projectId, fileName)
  }
  return mockData.projectScoreDrafts[projectId]
}

function normalizeScoreDraft(draft = {}) {
  const deliverables = Array.isArray(draft.suggestedDeliverables)
    ? draft.suggestedDeliverables
    : (() => {
        if (typeof draft.suggestedDeliverables !== 'string' || draft.suggestedDeliverables.trim() === '') {
          return []
        }
        try {
          const parsed = JSON.parse(draft.suggestedDeliverables)
          return Array.isArray(parsed) ? parsed : []
        } catch {
          return []
        }
      })()

  const dueDate = typeof draft.dueDate === 'string' && draft.dueDate.length >= 10
    ? draft.dueDate.slice(0, 10) + 'T00:00:00'
    : draft.dueDate || ''

  return {
    ...draft,
    category: draft.category || 'unknown',
    suggestedDeliverables: deliverables,
    dueDate,
    status: draft.status || 'DRAFT',
    sourceFileName: draft.sourceFileName || '',
    sourceTableIndex: Number.isFinite(Number(draft.sourceTableIndex)) ? Number(draft.sourceTableIndex) : null,
    sourceRowIndex: Number.isFinite(Number(draft.sourceRowIndex)) ? Number(draft.sourceRowIndex) : null,
  }
}

function normalizeScoreDraftList(drafts = []) {
  return Array.isArray(drafts) ? drafts.map((draft) => normalizeScoreDraft(draft)) : []
}

export const projectsApi = {
  /**
   * 获取项目列表
   */
  async getList(params) {
    if (isMockMode()) {
      mockData.projects = loadDemoState('projects', mockData.projects)
      return new Promise((resolve) => {
        setTimeout(() => {
          const data = applyProjectFilters([...mockData.projects], params)
          resolve({ success: true, data, total: data.length })
        }, 200)
      })
    }

    const response = await httpClient.get('/api/projects')
    const projects = Array.isArray(response?.data) ? response.data : []
    const data = applyProjectFilters(projects, params)

    return {
      ...response,
      data,
      total: data.length
    }
  },

  /**
   * 获取项目详情
   */
  async getDetail(id) {
    if (isMockMode()) {
      mockData.projects = loadDemoState('projects', mockData.projects)
      return new Promise((resolve) => {
        setTimeout(() => {
          const project = mockData.projects.find(p => p.id === id)
          resolve({ success: true, data: project })
        }, 100)
      })
    }

    if (!isNumericId(id)) {
      return apiModeFailure('project')
    }

    const response = await httpClient.get(`/api/projects/${id}`)
    return {
      ...response,
      data: response?.data ?? null
    }
  },

  /**
   * 创建项目
   */
  async create(data) {
    if (isMockMode()) {
      mockData.projects = loadDemoState('projects', mockData.projects)
      mockData.customerInsights = loadDemoState('customer-insights', mockData.customerInsights || [])
      mockData.customerPredictions = loadDemoState('customer-predictions', mockData.customerPredictions || [])

      const newProject = {
        ...data,
        id: 'P' + Date.now(),
        createTime: new Date().toISOString().split('T')[0]
      }
      mockData.projects.unshift(newProject)

      if (newProject.sourceModule === 'customer-opportunity-center' && newProject.sourceOpportunityId) {
        const prediction = (mockData.customerPredictions || []).find(
          item => item.opportunityId === newProject.sourceOpportunityId
        )
        if (prediction) {
          prediction.convertedProjectId = newProject.id
        }

        const insight = (mockData.customerInsights || []).find(
          item => item.customerId === newProject.sourceCustomerId || item.customerName === newProject.sourceCustomer
        )
        if (insight) {
          insight.status = 'converted'
        }
      }

      saveDemoState('projects', mockData.projects)
      saveDemoState('customer-insights', mockData.customerInsights || [])
      saveDemoState('customer-predictions', mockData.customerPredictions || [])

      return Promise.resolve({
        success: true,
        data: newProject
      })
    }
    return httpClient.post('/api/projects', data)
  },

  /**
   * 更新项目
   */
  async update(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { ...data, id } })
    }

    if (!isNumericId(id)) {
      return apiModeFailure('project')
    }

    return httpClient.put(`/api/projects/${id}`, data)
  },

  /**
   * 删除项目
   */
  async delete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }

    if (!isNumericId(id)) {
      return apiModeFailure('project')
    }

    return httpClient.delete(`/api/projects/${id}`)
  },

  /**
   * 获取项目任务
   */
  async getTasks(projectId) {
    if (isMockMode()) {
      const project = getMockProject(projectId)
      return Promise.resolve({ success: true, data: project?.tasks || [] })
    }

    if (!isNumericId(projectId)) {
      return apiModeFailure('project')
    }

    return httpClient.get(`/api/projects/${projectId}/tasks`)
  },

  async createTask(projectId, data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          id: `TASK_${Date.now()}`,
          ...data,
          status: data?.status || 'todo',
        }
      })
    }

    if (!isNumericId(projectId)) {
      return apiModeFailure('project')
    }

    return httpClient.post(`/api/projects/${projectId}/tasks`, data)
  },

  async updateTaskStatus(projectId, taskId, status) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: { id: taskId, status }
      })
    }

    if (!isNumericId(projectId) || !isNumericId(taskId)) {
      return apiModeFailure('task')
    }

    return httpClient.patch(`/api/projects/${projectId}/tasks/${taskId}/status`, { status })
  },

  /**
   * 获取项目文档
   */
  async getDocuments(projectId) {
    if (isMockMode()) {
      const project = getMockProject(projectId)
      return Promise.resolve({ success: true, data: project?.documents || [] })
    }

    if (!isNumericId(projectId)) {
      return apiModeFailure('project')
    }

    return httpClient.get(`/api/projects/${projectId}/documents`)
  },

  /**
   * 上传文档
   */
  async uploadDocument(projectId, formData) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: { id: 'D' + Date.now(), name: formData.get('file').name }
      })
    }

    if (!isNumericId(projectId)) {
      return apiModeFailure('project')
    }

    return httpClient.post(`/api/projects/${projectId}/documents`, {
      name: formData.get('name') || formData.get('file')?.name || '项目文档',
      size: formData.get('size') || '1MB',
      fileType: formData.get('fileType') || formData.get('file')?.type || 'application/octet-stream',
      uploaderId: formData.get('uploaderId') ? Number(formData.get('uploaderId')) : null,
      uploaderName: formData.get('uploaderName') || '',
    })
  },

  async deleteDocument(projectId, documentId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }

    if (!isNumericId(projectId) || !isNumericId(documentId)) {
      return apiModeFailure('document')
    }

    return httpClient.delete(`/api/projects/${projectId}/documents/${documentId}`)
  },

  async createReminder(projectId, data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: { id: `REM_${Date.now()}`, projectId, ...data }
      })
    }

    if (!isNumericId(projectId)) {
      return apiModeFailure('project')
    }

    return httpClient.post(`/api/projects/${projectId}/reminders`, data)
  },

  async createShareLink(projectId, data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          id: `SHARE_${Date.now()}`,
          projectId,
          token: `mock-${Date.now()}`,
          url: `${data?.baseUrl || window.location.origin}/project/${projectId}`,
          ...data,
        }
      })
    }

    if (!isNumericId(projectId)) {
      return apiModeFailure('project')
    }

    return httpClient.post(`/api/projects/${projectId}/share-links`, data)
  },

  async parseScoreDrafts(projectId, formData) {
    if (isMockMode()) {
      const fileName = formData.get('file')?.name || '评分标准示例.docx'
      const drafts = buildMockScoreDrafts(projectId, fileName)
      mockData.projectScoreDrafts[projectId] = drafts
      return Promise.resolve({
        success: true,
        data: {
          drafts: clone(drafts),
          totalCount: drafts.length,
          draftCount: drafts.filter(item => item.status === 'DRAFT').length,
          readyCount: drafts.filter(item => item.status === 'READY').length,
          skippedCount: drafts.filter(item => item.status === 'SKIPPED').length,
        }
      })
    }

    if (!isNumericId(projectId)) {
      return apiModeFailure('project')
    }

    return httpClient.post(`/api/projects/${projectId}/score-drafts/parse`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }).then((response) => ({
      ...response,
      data: response?.data
        ? {
            ...response.data,
            drafts: normalizeScoreDraftList(response.data.drafts)
          }
        : response?.data
    }))
  },

  async getScoreDrafts(projectId) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: clone(ensureMockScoreDrafts(projectId))
      })
    }

    if (!isNumericId(projectId)) {
      return apiModeFailure('project')
    }

    return httpClient.get(`/api/projects/${projectId}/score-drafts`).then((response) => ({
      ...response,
      data: normalizeScoreDraftList(Array.isArray(response?.data) ? response.data : [])
    }))
  },

  async updateScoreDraft(projectId, draftId, payload) {
    if (isMockMode()) {
      const drafts = ensureMockScoreDrafts(projectId)
      const index = drafts.findIndex(item => String(item.id) === String(draftId))
      if (index === -1) {
        return Promise.resolve({ success: false, message: '未找到评分草稿项' })
      }

      const next = {
        ...drafts[index],
        ...payload,
      }
      if (!payload.status) {
        next.status = next.assigneeId || next.assigneeName ? 'READY' : 'DRAFT'
      }
      drafts[index] = next
      mockData.projectScoreDrafts[projectId] = drafts
      return Promise.resolve({ success: true, data: clone(next) })
    }

    if (!isNumericId(projectId) || !isNumericId(draftId)) {
      return apiModeFailure('project score draft')
    }

    return httpClient.patch(`/api/projects/${projectId}/score-drafts/${draftId}`, payload).then((response) => ({
      ...response,
      data: response?.data ? normalizeScoreDraft(response.data) : response?.data
    }))
  },

  async generateScoreDraftTasks(projectId, draftIds) {
    if (isMockMode()) {
      const drafts = ensureMockScoreDrafts(projectId)
      const selectedDrafts = drafts.filter(item => draftIds.includes(item.id))
      const tasks = selectedDrafts.map((draft, index) => ({
        id: `TASK_${Date.now()}_${index}`,
        projectId,
        name: draft.generatedTaskTitle,
        description: draft.generatedTaskDescription,
        assigneeId: draft.assigneeId,
        owner: draft.assigneeName || '待分配',
        assignee: draft.assigneeName || '待分配',
        department: '投标管理部',
        status: 'todo',
        priority: draft.scoreValueText?.includes('10') || draft.scoreValueText?.includes('最高') ? 'high' : 'medium',
        dueDate: draft.dueDate ? String(draft.dueDate).slice(0, 10) : '',
        deliverables: (draft.suggestedDeliverables || []).map((name, deliverableIndex) => ({
          id: `${draft.id}_DEL_${deliverableIndex}`,
          name,
          url: '#',
        })),
        hasDeliverable: Array.isArray(draft.suggestedDeliverables) && draft.suggestedDeliverables.length > 0,
      }))

      mockData.projectScoreDrafts[projectId] = drafts.map((draft) => (
        draftIds.includes(draft.id)
          ? { ...draft, status: 'GENERATED' }
          : draft
      ))
      return Promise.resolve({ success: true, data: tasks })
    }

    if (!isNumericId(projectId)) {
      return apiModeFailure('project')
    }

    return httpClient.post(`/api/projects/${projectId}/score-drafts/generate-tasks`, { draftIds })
  },

  async clearScoreDrafts(projectId) {
    if (isMockMode()) {
      mockData.projectScoreDrafts[projectId] = []
      return Promise.resolve({ success: true, data: null })
    }

    if (!isNumericId(projectId)) {
      return apiModeFailure('project')
    }

    return httpClient.delete(`/api/projects/${projectId}/score-drafts`)
  }
}

export default projectsApi
