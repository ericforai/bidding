// Input: httpClient and bid result workflow APIs
// Output: bidResultsApi
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'
import {
  normalizeCompetitorRecord,
  normalizeCompetitorReport,
  normalizeDetail,
  normalizeFetchResult,
  normalizeOverview,
  normalizeReminder
} from './bidResults.normalizers.js'

const normalizeList = (data, mapper) => (Array.isArray(data) ? data.map(mapper) : [])

const createResultPayload = (payload = {}) => {
  const body = {
    projectId: payload.projectId ? Number(payload.projectId) : null,
    result: payload.result || 'lost',
    amount: payload.amount ?? null,
    contractStartDate: payload.contractStartDate || null,
    contractEndDate: payload.contractEndDate || null,
    contractDurationMonths: payload.contractDurationMonths ?? null,
    remark: payload.remark || '',
    skuCount: payload.skuCount ?? null
  }

  if (payload.winAnnounceDocUrl) {
    body.winAnnounceDocUrl = payload.winAnnounceDocUrl
  }

  if (payload.attachmentType) {
    body.attachmentType = payload.attachmentType
  }

  if (payload.attachmentDocumentId) {
    body.attachmentDocumentId = payload.attachmentDocumentId
  }

  return body
}

const createCompetitorPayload = (payload = {}) => ({
  competitorId: payload.competitorId ?? null,
  competitorName: payload.company || payload.competitorName || '',
  projectId: payload.projectId ? Number(payload.projectId) : null,
  skuCount: payload.skuCount ? Number(payload.skuCount) : null,
  category: payload.category || '',
  discount: payload.discount || '',
  paymentTerms: payload.paymentTerms || payload.payment || '',
  wonAt: payload.wonAt || null,
  amount: payload.amount ?? null,
  notes: payload.notes || payload.remark || ''
})

const isMissingEndpoint = (error) => {
  const status = error?.response?.status
  return status === 404 || status === 405
}

const normalizeAttachmentType = (value) => {
  if (value === 'WIN_NOTICE') return 'NOTICE'
  if (value === 'LOSS_REPORT') return 'REPORT'
  return value
}

export const bidResultsApi = {
  async getOverview() {
    const response = await httpClient.get('/api/bid-results/overview')
    return { ...response, data: normalizeOverview(response?.data) }
  },

  async sync() {
    return httpClient.post('/api/bid-results/sync')
  },

  async fetch() {
    return httpClient.post('/api/bid-results/fetch')
  },

  async getFetchResults() {
    const response = await httpClient.get('/api/bid-results/fetch-results')
    return { ...response, data: normalizeList(response?.data, normalizeFetchResult) }
  },

  async confirm(id) {
    const response = await httpClient.post(`/api/bid-results/fetch-results/${id}/confirm`)
    return { ...response, data: normalizeFetchResult(response?.data) }
  },

  async confirmWithData(id, payload = {}) {
    try {
      const response = await httpClient.post(`/api/bid-results/fetch-results/${id}/confirm-with-data`, createResultPayload(payload))
      return { ...response, data: normalizeFetchResult(response?.data) }
    } catch (error) {
      if (!isMissingEndpoint(error)) {
        throw error
      }

      const confirmed = await this.confirm(id)
      if (!payload || Object.keys(payload).length === 0) {
        return confirmed
      }
      const response = await httpClient.post(`/api/bid-results/${confirmed?.data?.id}/update`, createResultPayload(payload))
      return { ...response, data: normalizeFetchResult(response?.data) }
    }
  },

  async ignore(id, comment = '') {
    return httpClient.post(`/api/bid-results/fetch-results/${id}/ignore`, { comment })
  },

  async confirmBatch(ids = []) {
    return httpClient.post('/api/bid-results/fetch-results/confirm-batch', { ids })
  },

  async register(payload = {}) {
    const response = await httpClient.post('/api/bid-results/register', createResultPayload(payload))
    return { ...response, data: normalizeFetchResult(response?.data) }
  },

  async update(id, payload = {}) {
    const response = await httpClient.post(`/api/bid-results/${id}/update`, createResultPayload(payload))
    return { ...response, data: normalizeFetchResult(response?.data) }
  },

  async getReminders() {
    const response = await httpClient.get('/api/bid-results/reminders')
    return { ...response, data: normalizeList(response?.data, normalizeReminder) }
  },

  async sendReminder(resultId, comment = '') {
    const response = await httpClient.post('/api/bid-results/reminders/send', { resultId, comment })
    return { ...response, data: normalizeReminder(response?.data) }
  },

  async sendReminderBatch(ids = [], comment = '') {
    return httpClient.post('/api/bid-results/reminders/send-batch', { ids, comment })
  },

  async markReminderUploaded(reminderId, payload = {}) {
    const response = await httpClient.post(`/api/bid-results/reminders/${reminderId}/mark-uploaded`, {
      documentId: payload.documentId ?? payload.projectDocumentId ?? null,
      attachmentType: normalizeAttachmentType(payload.attachmentType)
    })
    return { ...response, data: normalizeReminder(response?.data) }
  },

  async uploadProjectDocument(projectId, payload = {}) {
    const file = payload.file || {}
    return httpClient.post(`/api/projects/${projectId}/documents`, {
      name: payload.name || file.name || '结果附件',
      size: payload.size || file.size || 0,
      fileType: payload.fileType || file.type || 'application/octet-stream',
      uploaderId: payload.uploaderId ?? null,
      uploaderName: payload.uploaderName || '',
      documentCategory: payload.documentCategory || '',
      linkedEntityType: payload.linkedEntityType || 'BID_RESULT',
      linkedEntityId: payload.linkedEntityId ?? null,
      fileUrl: payload.fileUrl || '',
      storageKey: payload.storageKey || ''
    })
  },

  async bindAttachment(resultId, payload = {}) {
    const response = await httpClient.post(`/api/bid-results/${resultId}/attachments/bind`, {
      documentId: payload.documentId ?? payload.projectDocumentId ?? null,
      attachmentType: normalizeAttachmentType(payload.attachmentType)
    })
    return { ...response, data: normalizeFetchResult(response?.data) }
  },

  async createCompetitorWin(payload = {}) {
    const response = await httpClient.post('/api/bid-results/competitor-wins', createCompetitorPayload(payload))
    return { ...response, data: normalizeCompetitorRecord(response?.data) }
  },

  async getDetail(id) {
    const response = await httpClient.get(`/api/bid-results/${id}`)
    return { ...response, data: normalizeDetail(response?.data) }
  },

  async getCompetitorReport() {
    const response = await httpClient.get('/api/bid-results/competitor-report')
    return { ...response, data: normalizeList(response?.data, normalizeCompetitorReport) }
  }
}

export default bidResultsApi
