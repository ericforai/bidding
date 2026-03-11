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
      const project = mockData.projects.find(p => p.id === projectId)
      return Promise.resolve({ success: true, data: project?.tasks || [] })
    }

    return Promise.resolve({
      success: false,
      message: 'Project task sub-route is not implemented on the backend yet'
    })
  },

  /**
   * 获取项目文档
   */
  async getDocuments(projectId) {
    if (isMockMode()) {
      const project = mockData.projects.find(p => p.id === projectId)
      return Promise.resolve({ success: true, data: project?.documents || [] })
    }

    return Promise.resolve({
      success: false,
      message: 'Project document sub-route is not implemented on the backend yet'
    })
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

    return Promise.resolve({
      success: false,
      message: 'Project document upload is not implemented on the backend yet'
    })
  }
}

export default projectsApi
