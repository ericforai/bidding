/**
 * 项目模块 API
 * 支持双模式切换
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

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

function getMockProjects(params = {}) {
  return applyProjectFilters([...mockData.projects], params)
}

function getMockProject(id) {
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

export const projectsApi = {
  /**
   * 获取项目列表
   */
  async getList(params) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const data = applyProjectFilters([...mockData.projects], params)
          resolve({ success: true, data, total: data.length })
        }, 200)
      })
    }

    const response = await httpClient.get('/api/projects')
    const projects = Array.isArray(response?.data) ? response.data : []
    const filteredData = applyProjectFilters(projects, params)
    const data = filteredData.length > 0 || projects.length > 0
      ? filteredData
      : getMockProjects(params)

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
      return new Promise((resolve) => {
        setTimeout(() => {
          const project = mockData.projects.find(p => p.id === id)
          resolve({ success: true, data: project })
        }, 100)
      })
    }

    if (!isNumericId(id)) {
      const mockProject = mockData.projects.find((project) => String(project.id) === String(id))
      return {
        success: Boolean(mockProject),
        data: mockProject || null,
        message: mockProject ? '使用演示项目数据' : 'Current backend only supports numeric project IDs in API mode'
      }
    }

    try {
      const response = await httpClient.get(`/api/projects/${id}`)
      return response?.data
        ? response
        : { ...response, data: mockData.projects.find((project) => String(project.id) === String(id)) || null }
    } catch (error) {
      const mockProject = mockData.projects.find((project) => String(project.id) === String(id))
      if (mockProject) {
        return { success: true, data: mockProject, message: '使用演示项目数据' }
      }
      throw error
    }
  },

  /**
   * 创建项目
   */
  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: { ...data, id: 'P' + Date.now(), createTime: new Date().toISOString().split('T')[0] }
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
      return {
        success: false,
        message: 'Current backend only supports numeric project IDs in API mode'
      }
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
      return {
        success: false,
        message: 'Current backend only supports numeric project IDs in API mode'
      }
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
      const project = getMockProject(projectId)
      return Promise.resolve({
        success: true,
        data: project?.tasks || [],
        message: '使用演示项目任务数据'
      })
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
      return Promise.resolve({
        success: false,
        message: 'Current backend only supports numeric project IDs in API mode'
      })
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
      return Promise.resolve({
        success: false,
        message: 'Current backend only supports numeric task IDs in API mode'
      })
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
      const project = getMockProject(projectId)
      return Promise.resolve({
        success: true,
        data: project?.documents || [],
        message: '使用演示项目文档数据'
      })
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
      return Promise.resolve({
        success: false,
        message: 'Current backend only supports numeric project IDs in API mode'
      })
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
      return Promise.resolve({
        success: false,
        message: 'Current backend only supports numeric document IDs in API mode'
      })
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
      return Promise.resolve({
        success: false,
        message: 'Current backend only supports numeric project IDs in API mode'
      })
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
      return Promise.resolve({
        success: false,
        message: 'Current backend only supports numeric project IDs in API mode'
      })
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

    return httpClient.post(`/api/projects/${projectId}/score-drafts/parse`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  async getScoreDrafts(projectId) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: clone(ensureMockScoreDrafts(projectId))
      })
    }

    return httpClient.get(`/api/projects/${projectId}/score-drafts`)
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

    return httpClient.patch(`/api/projects/${projectId}/score-drafts/${draftId}`, payload)
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

    return httpClient.post(`/api/projects/${projectId}/score-drafts/generate-tasks`, { draftIds })
  },

  async clearScoreDrafts(projectId) {
    if (isMockMode()) {
      mockData.projectScoreDrafts[projectId] = []
      return Promise.resolve({ success: true, data: null })
    }

    return httpClient.delete(`/api/projects/${projectId}/score-drafts`)
  }
}

export default projectsApi
