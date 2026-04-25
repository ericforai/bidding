// Input: httpClient and WeChat Work integration endpoint payloads
// Output: weComIntegrationApi with getConfig / saveConfig / testConnection
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'

const BASE = '/api/admin/integrations/wecom'

export const weComIntegrationApi = {
  async getConfig() {
    return httpClient.get(BASE)
  },

  async saveConfig(payload) {
    return httpClient.put(BASE, payload)
  },

  async testConnection() {
    return httpClient.post(`${BASE}/test`)
  },
}

export default weComIntegrationApi
