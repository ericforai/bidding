// Input: httpClient, API mode config, fee normalizers and mock sources
// Output: feesApi - fee application, approval, and refund accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 费用管理模块 API
 * 支持双模式切换
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

export const feesApi = {
  /**
   * 获取费用列表
   */
  async getList(params) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          let data = [...mockData.fees]
          if (params?.status) {
            data = data.filter(f => f.status === params.status)
          }
          if (params?.project) {
            data = data.filter(f => f.project.includes(params.project))
          }
          resolve({ success: true, data, total: data.length })
        }, 200)
      })
    }
    return httpClient.get('/api/fees', { params })
  },

  /**
   * 获取费用详情
   */
  async getDetail(id) {
    if (isMockMode()) {
      const item = mockData.fees.find(f => f.id === id)
      return Promise.resolve({ success: true, data: item })
    }
    return httpClient.get(`/api/fees/${id}`)
  },

  /**
   * 创建费用记录
   */
  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { ...data, id: 'F' + Date.now() } })
    }
    return httpClient.post('/api/fees', data)
  },

  /**
   * 缴纳费用
   */
  async pay(id, paymentData) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { id, status: 'paid' } })
    }
    return httpClient.post(`/api/fees/${id}/pay`, paymentData)
  },

  /**
   * 退还保证金
   */
  async return(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { id, status: 'returned' } })
    }
    return httpClient.post(`/api/fees/${id}/return`)
  },

  /**
   * 获取统计数据
   */
  async getStatistics() {
    if (isMockMode()) {
      const totalPaid = mockData.fees.filter(f => f.status === 'paid').reduce((sum, f) => sum + f.amount, 0)
      const totalPending = mockData.fees.filter(f => f.status === 'pending').reduce((sum, f) => sum + f.amount, 0)
      return Promise.resolve({
        success: true,
        data: { totalPaid, totalPending, total: totalPaid + totalPending }
      })
    }
    return httpClient.get('/api/fees/statistics')
  }
}

export default feesApi
