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
  }
}

export default projectsApi
