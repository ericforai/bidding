// Input: httpClient and batch operation endpoints
// Output: batchApi - batch claim and assignment accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 批量操作模块 API
 * 真实 API 为唯一数据源
 */
import httpClient from '../client.js'

export const batchApi = {
  async claimTenders(itemIds, userId, itemType = 'tender') {
    return httpClient.post('/api/batch/tenders/claim', { itemIds, userId, itemType })
  },

  async assignTasks(taskIds, assignment = {}, remark = '') {
    const payload = typeof assignment === 'object' && assignment !== null
      ? {
          ...assignment,
          taskIds,
          remark: assignment.remark ?? remark
        }
      : {
          taskIds,
          assigneeId: assignment,
          remark
        }

    return httpClient.post('/api/batch/tasks/assign', payload)
  }
}

export default batchApi
