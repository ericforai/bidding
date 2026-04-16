// Input: httpClient and tender endpoints
// Output: tendersApi - tender list, detail, and conversion accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 标讯模块 API
 * 真实 API 为唯一数据源
 */
import httpClient from '../client.js'

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

export const tendersApi = {
  async getList(params) {
    const response = await httpClient.get('/api/tenders')
    const tenders = Array.isArray(response?.data) ? response.data : []
    const data = applyTenderFilters(tenders, params)

    return {
      ...response,
      data,
      total: data.length
    }
  },

  async getDetail(id) {
    if (!isNumericId(id)) {
      return {
        success: false,
        data: null,
        message: '当前后端仅支持数字型标讯 ID'
      }
    }

    return httpClient.get(`/api/tenders/${id}`)
  },

  async create(data) {
    return httpClient.post('/api/tenders', data)
  },

  async update(id, data) {
    if (!isNumericId(id)) {
      return {
        success: false,
        message: '当前后端仅支持数字型标讯 ID'
      }
    }

    return httpClient.put(`/api/tenders/${id}`, data)
  },

  async delete(id) {
    if (!isNumericId(id)) {
      return {
        success: false,
        message: '当前后端仅支持数字型标讯 ID'
      }
    }

    return httpClient.delete(`/api/tenders/${id}`)
  },

  async getAIAnalysis(id) {
    if (!isNumericId(id)) {
      return {
        success: false,
        message: '当前后端仅支持数字型标讯 ID'
      }
    }

    try {
      return await httpClient.get(`/api/tenders/${id}/ai-analysis`)
    } catch (error) {
      if (error?.response?.status !== 404) {
        throw error
      }
      return httpClient.post(`/api/tenders/${id}/ai-analysis`)
    }
  }
}

export default tendersApi

export const crawlerApi = {
  async trigger({ keyword = '', page = 1, pageSize = 20 } = {}) {
    return httpClient.post('/api/admin/crawler/trigger', null, {
      params: { keyword, page, pageSize }
    })
  }
}
