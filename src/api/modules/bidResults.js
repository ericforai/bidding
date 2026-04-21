// Input: httpClient and bid result delivery APIs
// Output: bidResultsApi - bid result query, registration, confirmation, attachment, and competitor accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'
import {
  normalizeCompetitorRecord,
  normalizeCompetitorReport,
  normalizeDetail,
  normalizeFetchResult,
  normalizeOverview,
  normalizeReminder,
} from './bidResults.normalizers.js'

const buildProjectDocumentPayload = (data = {}) => {
  const file = data.file || null
  const rawSize = data.size ?? file?.size
  const size = rawSize == null ? '' : String(rawSize)

  return {
    name: data.name || file?.name || '投标结果附件',
    size,
    fileType: data.fileType || file?.type || 'application/octet-stream',
    documentCategory: data.documentCategory || '',
    linkedEntityType: data.linkedEntityType || '',
    linkedEntityId: data.linkedEntityId ?? null,
    fileUrl: data.fileUrl || '',
    uploaderId: data.uploaderId ?? null,
    uploaderName: data.uploaderName || '',
  }
}

const buildCompetitorWinPayload = (data = {}) => ({
  competitorId: data.competitorId ?? null,
  competitorName: data.competitorName || data.company || '',
  projectId: data.projectId ?? null,
  skuCount: data.skuCount ?? null,
  category: data.category || '',
  discount: data.discount || '',
  paymentTerms: data.paymentTerms || data.payment || '',
  wonAt: data.wonAt || null,
  amount: data.amount ?? null,
  notes: data.notes || data.remark || '',
})

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
    return { ...response, data: Array.isArray(response?.data) ? response.data.map(normalizeFetchResult) : [] }
  },

  async register(data) {
    const response = await httpClient.post('/api/bid-results/register', data)
    return { ...response, data: normalizeFetchResult(response?.data) }
  },

  async update(id, data) {
    const response = await httpClient.post(`/api/bid-results/${id}/update`, data)
    return { ...response, data: normalizeFetchResult(response?.data) }
  },

  async confirm(id, data = {}) {
    return this.confirmWithData(id, data)
  },

  async confirmWithData(id, data = {}) {
    const response = await httpClient.post(`/api/bid-results/fetch-results/${id}/confirm-with-data`, data)
    return { ...response, data: normalizeFetchResult(response?.data) }
  },

  async ignore(id, comment = '') {
    return httpClient.post(`/api/bid-results/fetch-results/${id}/ignore`, { comment })
  },
  async confirmBatch(ids = []) {
    return httpClient.post('/api/bid-results/fetch-results/confirm-batch', { ids })
  },
  async getReminders() {
    const response = await httpClient.get('/api/bid-results/reminders')
    return { ...response, data: Array.isArray(response?.data) ? response.data.map(normalizeReminder) : [] }
  },
  async sendReminder(resultId, comment = '') {
    const response = await httpClient.post('/api/bid-results/reminders/send', { resultId, comment })
    return { ...response, data: normalizeReminder(response?.data) }
  },
  async sendReminderBatch(ids = [], comment = '') {
    return httpClient.post('/api/bid-results/reminders/send-batch', { ids, comment })
  },
  async markReminderUploaded(reminderId, data) {
    const response = await httpClient.post(`/api/bid-results/reminders/${reminderId}/mark-uploaded`, data)
    return { ...response, data: normalizeReminder(response?.data) }
  },
  async getDetail(id) {
    const response = await httpClient.get(`/api/bid-results/${id}`)
    return { ...response, data: normalizeDetail(response?.data) }
  },
  async getCompetitorReport() {
    const response = await httpClient.get('/api/bid-results/competitor-report')
    return { ...response, data: Array.isArray(response?.data) ? response.data.map(normalizeCompetitorReport) : [] }
  },
  async createCompetitorWin(data) {
    const response = await httpClient.post('/api/bid-results/competitor-wins', buildCompetitorWinPayload(data))
    return { ...response, data: normalizeCompetitorRecord(response?.data) }
  },
  async uploadProjectDocument(projectId, data) {
    return httpClient.post(`/api/projects/${projectId}/documents`, buildProjectDocumentPayload(data))
  },
  async bindAttachment(resultId, data) {
    const response = await httpClient.post(`/api/bid-results/${resultId}/attachments/bind`, data)
    return { ...response, data: normalizeFetchResult(response?.data) }
  } }

export default bidResultsApi
