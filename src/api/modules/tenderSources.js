// Input: httpClient and tender source test endpoint
// Output: tenderSourcesApi - tender source configuration API accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 标讯源配置模块 API
 * 真实 API 为唯一数据源
 */
import httpClient from '../client.js'

const BASE = '/api/tender-sources'

export const tenderSourcesApi = {
  /**
   * 测试标讯源连接
   * @param {Object} params - { platform, apiEndpoint, apiKey }
   * @returns {Promise<{success: boolean, message: string, data: {success: boolean, message: string}}>}
   */
  async testConnection(params) {
    return httpClient.post(`${BASE}/test-connection`, params)
  }
}

export default tenderSourcesApi
