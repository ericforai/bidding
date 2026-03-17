import httpClient from '../client.js'

export const auditApi = {
  async getLogs(params = {}) {
    return httpClient.get('/api/audit', { params })
  },
}

export default auditApi
