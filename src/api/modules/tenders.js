// Input: httpClient, API mode config, tender normalizers and mock adapters
// Output: tendersApi - tender list, detail, and conversion accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 标讯模块 API
 * 支持双模式切换
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

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
    const data = filteredData.length > 0 || tenders.length > 0
      ? filteredData
      : getMockTenders(params)

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
      const mockTender = mockData.tenders.find((tender) => String(tender.id) === String(id))
      return {
        success: Boolean(mockTender),
        data: mockTender || null,
        message: mockTender ? '使用演示标讯数据' : 'Current backend only supports numeric tender IDs in API mode'
      }
    }

    try {
      const response = await httpClient.get(`/api/tenders/${id}`)
      return response?.data
        ? response
        : { ...response, data: mockData.tenders.find((tender) => String(tender.id) === String(id)) || null }
    } catch (error) {
      const mockTender = mockData.tenders.find((tender) => String(tender.id) === String(id))
      if (mockTender) {
        return { success: true, data: mockTender, message: '使用演示标讯数据' }
      }
      throw error
    }
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
      return {
        success: false,
        message: 'Current backend only supports numeric tender IDs in API mode'
      }
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
      return {
        success: false,
        message: 'Current backend only supports numeric tender IDs in API mode'
      }
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
      success: false,
      message: 'Tender AI analysis payload is not aligned with the backend response yet'
    })
  }
}

export default tendersApi
