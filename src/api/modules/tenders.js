// Input: httpClient, tender endpoints, and doc-insight parse endpoint
// Output: tendersApi - tender list, detail, upload, and manual intake parse accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 标讯模块 API
 * 真实 API 为唯一数据源
 */
import httpClient from '../client.js'

function normalizeTags(tags) {
  if (Array.isArray(tags)) {
    return tags
  }
  if (typeof tags === 'string' && tags.trim()) {
    return tags.split(',').map(tag => tag.trim()).filter(Boolean)
  }
  return []
}

function generateIdempotencyKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `idem-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function withIdempotencyKey(config = {}) {
  return {
    ...config,
    headers: {
      ...(config.headers || {}),
      'Idempotency-Key': generateIdempotencyKey()
    }
  }
}

function normalizeTenderRecord(tender = {}) {
  return {
    ...tender,
    tags: normalizeTags(tender.tags)
  }
}

function isNumericId(id) {
  return /^\d+$/.test(String(id))
}

export const tendersApi = {
  async getList(params = {}) {
    const response = await httpClient.get('/api/tenders', { params })
    const data = Array.isArray(response?.data) ? response.data.map(normalizeTenderRecord) : []

    return {
      ...response,
      data,
      total: response?.total ?? data.length
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
    return httpClient.post('/api/tenders', data, withIdempotencyKey())
  },

  async downloadImportTemplate() {
    return httpClient.get('/api/tenders/import-template', {
      responseType: 'blob',
      timeout: 60000
    })
  },

  async bulkImport(file) {
    const formData = new FormData()
    formData.set('file', file, file?.name || 'tender-import.xlsx')
    return httpClient.post('/api/tenders/import', formData, withIdempotencyKey({
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }))
  },

  async parseTenderIntakeDocument(file, { entityId = 'manual-tender' } = {}) {
    const formData = new FormData()
    formData.set('profile', 'TENDER_INTAKE')
    formData.set('entityId', entityId)
    formData.set('file', file, file?.name || 'manual-tender-document')

    return httpClient.post('/api/doc-insight/parse', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 45000
    })
  },

  async parseTenderIntakeText(text, { entityId = 'manual-tender' } = {}) {
    const file = new File([String(text || '')], '粘贴标讯文本.txt', { type: 'text/plain' })
    return tendersApi.parseTenderIntakeDocument(file, { entityId })
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
  },

  async initUploadSession(data) {
    return httpClient.post('/api/tenders/upload-init', data)
  },

  async completeUpload(data) {
    return httpClient.post('/api/tenders/upload-complete', data)
  },

  async getUploadTaskStatus(taskId) {
    if (!isNumericId(taskId)) {
      return {
        success: false,
        message: '当前后端仅支持数字型任务 ID'
      }
    }
    return httpClient.get(`/api/tenders/tasks/${taskId}`)
  },

  async getEvaluation(tenderId) {
    if (!isNumericId(tenderId)) {
      return {
        success: false,
        message: '当前后端仅支持数字型标讯 ID'
      }
    }
    return httpClient.get(`/api/tenders/${tenderId}/evaluation`)
  },

  async submitEvaluation(tenderId, data) {
    if (!isNumericId(tenderId)) {
      return {
        success: false,
        message: '当前后端仅支持数字型标讯 ID'
      }
    }
    return httpClient.post(`/api/tenders/${tenderId}/evaluation`, data)
  },

  async reviewTender(tenderId, data) {
    if (!isNumericId(tenderId)) {
      return {
        success: false,
        message: '当前后端仅支持数字型标讯 ID'
      }
    }
    return httpClient.post(`/api/tenders/${tenderId}/review`, data)
  },

  async proceedToBid(tenderId) {
    if (!isNumericId(tenderId)) {
      return {
        success: false,
        message: '当前后端仅支持数字型标讯 ID'
      }
    }
    return httpClient.post(`/api/tenders/${tenderId}/bid`)
  }
}

export default tendersApi
