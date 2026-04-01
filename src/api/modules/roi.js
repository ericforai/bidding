// Input: httpClient, ROI analysis service
// Output: roiApi
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * ROI分析模块 API
 * 支持双模式切换: Mock 数据 / 真实后端 API
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

function normalizeROI(item) {
  return {
    id: item?.id,
    projectId: item?.projectId || null,
    projectName: item?.projectName || '',
    analysisDate: item?.analysisDate || '',
    investment: item?.investment || item?.cost || 0,
    revenue: item?.revenue || 0,
    profit: item?.profit || 0,
    roiPercentage: item?.roiPercentage || item?.roi || 0,
    paybackMonths: item?.paybackMonths || 0,
    npv: item?.npv || 0,
    irr: item?.irr || 0,
    riskLevel: item?.riskLevel || 'MEDIUM',
    sensitivityData: item?.sensitivityData || null,
    assumptions: item?.assumptions || '',
    createdBy: item?.createdBy || '',
    createdAt: item?.createdAt || '',
  }
}

export const roiApi = {
  async getAnalyses(projectId) {
    if (isMockMode()) {
      const data = projectId
        ? (mockData.roiAnalyses || []).filter((r) => r.projectId === projectId).map(normalizeROI)
        : (mockData.roiAnalyses || []).map(normalizeROI)
      return Promise.resolve({ success: true, data })
    }
    const url = projectId ? `/api/ai/roi/project/${projectId}` : '/api/ai/roi'
    const response = await httpClient.get(url)
    return {
      ...response,
      data: response?.data ? normalizeROI(response.data) : null,
    }
  },

  async getAnalysis(id) {
    if (isMockMode()) {
      const item = (mockData.roiAnalyses || []).find((r) => String(r.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeROI(item) : null })
    }
    const response = await httpClient.get(`/api/ai/roi/${id}`)
    return { ...response, data: normalizeROI(response?.data) }
  },

  async createAnalysis(data) {
    if (isMockMode()) {
      const investment = Number(data.investment || data.cost || 0)
      const revenue = Number(data.revenue || 0)
      const profit = revenue - investment
      const roiPercentage = investment > 0 ? (profit / investment) * 100 : 0
      return Promise.resolve({
        success: true,
        data: normalizeROI({
          ...data,
          id: `ROI${Date.now()}`,
          investment,
          revenue,
          profit,
          roiPercentage,
        }),
      })
    }
    const response = await httpClient.post('/api/ai/roi', data)
    return { ...response, data: normalizeROI(response?.data) }
  },

  async updateAnalysis(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: normalizeROI({ ...data, id }) })
    }
    const response = await httpClient.put(`/api/ai/roi/${id}`, data)
    return { ...response, data: normalizeROI({ ...response?.data, ...data }) }
  },

  async deleteAnalysis(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    return httpClient.delete(`/api/ai/roi/${id}`)
  },

  async getSensitivity(projectId) {
    if (isMockMode()) {
      const data = { scenarios: [] }
      return Promise.resolve({ success: true, data })
    }
    const response = await httpClient.get(`/api/ai/roi/${projectId}/sensitivity`)
    return { ...response, data: response?.data }
  },
}

export default {
  roi: roiApi,
}
