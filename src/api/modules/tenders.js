/**
 * 标讯模块 API
 * 支持双模式切换
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'
import { buildFeatureUnavailableResponse } from '../featureAvailability.js'

function matchesTenderField(actualValue, expectedValue) {
  return String(actualValue || '').toLowerCase() === String(expectedValue || '').toLowerCase()
}

function applyTenderFilters(tenders, params = {}) {
  return tenders.filter((tender) => {
    if (params.status && !matchesTenderField(tender.status, params.status)) {
      return false
    }

    if (params.industry && !matchesTenderField(tender.industry, params.industry)) {
      return false
    }

    if (params.source && !matchesTenderField(tender.source, params.source)) {
      return false
    }

    if (params.keyword) {
      const keyword = String(params.keyword).trim().toLowerCase()
      if (!String(tender.title || '').toLowerCase().includes(keyword)) {
        return false
      }
    }

    return true
  })
}

function isNumericId(id) {
  return /^\d+$/.test(String(id))
}

function getMockTenders(params = {}) {
  return applyTenderFilters([...mockData.tenders], params)
}

function invalidApiModeId(entityName) {
  return buildFeatureUnavailableResponse({
    feature: `${entityName}-numeric-id`,
    title: '当前 ID 格式暂未接入',
    message: `Current backend only supports numeric ${entityName} IDs in API mode`,
    hint: '请使用真实后端返回的数字 ID 访问该资源。',
    scope: 'action',
  })
}

export const tendersApi = {
  /**
   * 获取标讯列表
   */
  async getList(params) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const data = applyTenderFilters([...mockData.tenders], params)
          resolve({ success: true, data, total: data.length })
        }, 200)
      })
    }

    const response = await httpClient.get('/api/tenders')
    const tenders = Array.isArray(response?.data) ? response.data : []
    const filteredData = applyTenderFilters(tenders, params)
    const data = filteredData

    return {
      ...response,
      data,
      total: data.length
    }
  },

  /**
   * 获取标讯详情
   */
  async getDetail(id) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const tender = mockData.tenders.find(t => t.id === id)
          resolve({ success: true, data: tender })
        }, 100)
      })
    }

    if (!isNumericId(id)) {
      return invalidApiModeId('tender')
    }

    return httpClient.get(`/api/tenders/${id}`)
  },

  /**
   * 创建标讯
   */
  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: { ...data, id: 'B' + Date.now() }
      })
    }
    return httpClient.post('/api/tenders', data)
  },

  /**
   * 更新标讯
   */
  async update(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { ...data, id } })
    }

    if (!isNumericId(id)) {
      return invalidApiModeId('tender')
    }

    return httpClient.put(`/api/tenders/${id}`, data)
  },

  /**
   * 删除标讯
   */
  async delete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }

    if (!isNumericId(id)) {
      return invalidApiModeId('tender')
    }

    return httpClient.delete(`/api/tenders/${id}`)
  },

  /**
   * 获取 AI 分析结果
   */
  async getAIAnalysis(id) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          resolve({
            success: true,
            data: mockData.aiAnalysis[id] || mockData.aiAnalysis.B001
          })
        }, 500)
      })
    }

    return Promise.resolve({
      ...buildFeatureUnavailableResponse({
        feature: 'tender-ai-analysis',
        title: '标讯 AI 分析暂未接入',
        message: 'Tender AI analysis payload is not aligned with the backend response yet',
        hint: '请先使用项目级 AI 分析能力，或等待后端补齐标讯分析接口。',
        scope: 'section',
      }),
    })
  },

  /**
   * 批量认领标讯
   */
  async batchClaim(tenderIds, userId) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const claimedTenders = tenderIds.map(id => {
            const tender = mockData.tenders.find(t => t.id === id)
            if (tender) {
              tender.status = 'following'
              tender.assignee = userId
            }
            return id
          })
          resolve({
            success: true,
            data: {
              claimed: claimedTenders.length,
              failed: 0,
              tenderIds: claimedTenders
            }
          })
        }, 300)
      })
    }

    return httpClient.post('/api/tenders/batch/claim', { tenderIds, userId })
  },

  /**
   * 批量分配标讯
   */
  async batchAssign(tenderIds, assigneeId) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const assignedTenders = tenderIds.map(id => {
            const tender = mockData.tenders.find(t => t.id === id)
            if (tender) {
              tender.assignee = assigneeId
              tender.status = 'contacted'
            }
            return id
          })
          resolve({
            success: true,
            data: {
              assigned: assignedTenders.length,
              failed: 0,
              tenderIds: assignedTenders
            }
          })
        }, 300)
      })
    }

    return httpClient.post('/api/tenders/batch/assign', { tenderIds, assigneeId })
  },

  /**
   * 批量更新标讯状态
   */
  async batchUpdateStatus(tenderIds, status) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          tenderIds.forEach(id => {
            const tender = mockData.tenders.find(t => t.id === id)
            if (tender) {
              tender.status = status
            }
          })
          resolve({
            success: true,
            data: {
              updated: tenderIds.length,
              failed: 0,
              tenderIds
            }
          })
        }, 300)
      })
    }

    return httpClient.post('/api/tenders/batch/status', { tenderIds, status })
  },

  /**
   * 批量删除标讯
   */
  async batchDelete(tenderIds) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          tenderIds.forEach(id => {
            const index = mockData.tenders.findIndex(t => t.id === id)
            if (index !== -1) {
              mockData.tenders.splice(index, 1)
            }
          })
          resolve({
            success: true,
            data: {
              deleted: tenderIds.length,
              failed: 0,
              tenderIds
            }
          })
        }, 300)
      })
    }

    return httpClient.post('/api/tenders/batch/delete', { tenderIds })
  }
}

export default tendersApi
