// Input: httpClient and tender batch endpoints
// Output: batchTendersApi - batch claim, assign, status, and assignment accessors
// Pos: src/api/modules/tenders/ - Frontend tender batch API layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../../client.js'

function normalizeBatchResponse(response = {}) {
  return {
    ...response,
    success: response?.success ?? true,
    data: response?.data || null,
  }
}

export const batchTendersApi = {
  async batchClaim(tenderIds, userId) {
    const response = await httpClient.post('/api/batch/tenders/claim', {
      itemIds: tenderIds,
      userId: Number(userId),
      itemType: 'tender',
    })
    return normalizeBatchResponse(response)
  },

  async batchAssign(tenderIds, assigneeId, remark = '') {
    const response = await httpClient.post('/api/batch/tenders/assign', {
      tenderIds,
      assigneeId: Number(assigneeId),
      remark,
    })
    return normalizeBatchResponse(response)
  },

  async batchUpdateStatus(tenderIds, status) {
    const response = await httpClient.patch('/api/batch/tenders/status', {
      tenderIds,
      status,
    })
    return normalizeBatchResponse(response)
  },

  async getAssignmentRecords(tenderId) {
    const response = await httpClient.get(`/api/tenders/${tenderId}/assignment`)
    return {
      ...response,
      data: response?.data || { latest: null, history: [] },
    }
  },

  async getAssignmentCandidates() {
    const response = await httpClient.get('/api/tenders/assignment-candidates')
    return {
      ...response,
      data: Array.isArray(response?.data) ? response.data : [],
    }
  },
}

export default batchTendersApi
