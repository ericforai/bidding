// Input: httpClient and audit query parameters from management views
// Output: auditApi - audit log retrieval functions
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'

export const auditApi = {
  async getLogs(params = {}) {
    return httpClient.get('/api/audit', { params })
  },
}

export default auditApi
