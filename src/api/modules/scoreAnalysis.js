// Input: httpClient, score analysis service
// Output: scoreAnalysisApi
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 评分分析模块 API
 * 支持双模式切换: Mock 数据 / 真实后端 API
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

function normalizeDimension(item) {
  return {
    dimension: item?.dimension || item?.name || '',
    score: item?.score || 0,
    weight: item?.weight || 1.0,
    maxScore: item?.maxScore || 100,
    comment: item?.comment || '',
  }
}

function normalizeScoreAnalysis(item) {
  return {
    id: item?.id,
    projectId: item?.projectId || null,
    projectName: item?.projectName || '',
    analysisDate: item?.analysisDate || '',
    totalScore: item?.totalScore || 0,
    riskLevel: item?.riskLevel || 'MEDIUM',
    dimensions: (item?.dimensions || []).map(normalizeDimension),
    recommendation: item?.recommendation || 'HOLD',
    strengths: item?.strengths || [],
    weaknesses: item?.weaknesses || [],
    comparisonNote: item?.comparisonNote || '',
    createdBy: item?.createdBy || '',
    createdAt: item?.createdAt || '',
  }
}

const DEFAULT_DIMENSIONS = [
  { dimension: '技术能力', weight: 1.0 },
  { dimension: '财务实力', weight: 1.0 },
  { dimension: '团队经验', weight: 1.0 },
  { dimension: '历史业绩', weight: 1.0 },
  { dimension: '合规性', weight: 1.0 },
  { dimension: '价格竞争力', weight: 1.0 },
]

export const scoreAnalysisApi = {
  async getAnalyses(projectId) {
    if (isMockMode()) {
      const data = projectId
        ? (mockData.scoreAnalyses || []).filter((s) => s.projectId === projectId).map(normalizeScoreAnalysis)
        : (mockData.scoreAnalyses || []).map(normalizeScoreAnalysis)
      return Promise.resolve({ success: true, data })
    }
    const url = projectId ? `/api/ai/score-analysis/project/${projectId}/history` : '/api/ai/score-analysis'
    const response = await httpClient.get(url)
    return {
      ...response,
      data: (response?.data || []).map(normalizeScoreAnalysis),
    }
  },

  async getAnalysis(id) {
    if (isMockMode()) {
      const item = (mockData.scoreAnalyses || []).find((s) => String(s.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeScoreAnalysis(item) : null })
    }
    const response = await httpClient.get(`/api/ai/score-analysis/${id}`)
    return { ...response, data: normalizeScoreAnalysis(response?.data) }
  },

  async getAnalysisByProject(projectId) {
    if (isMockMode()) {
      const item = (mockData.scoreAnalyses || []).find((s) => s.projectId === projectId)
      return Promise.resolve({ success: true, data: item ? normalizeScoreAnalysis(item) : null })
    }
    const response = await httpClient.get(`/api/ai/score-analysis/project/${projectId}`)
    return { ...response, data: normalizeScoreAnalysis(response?.data) }
  },

  async createAnalysis(data) {
    if (isMockMode()) {
      const dimensions = (data.dimensions || DEFAULT_DIMENSIONS).map((d) => ({
        ...d,
        score: Number(d.score || 0),
        weight: Number(d.weight || 1.0),
      }))
      const totalScore = dimensions.reduce((sum, d) => sum + d.score * d.weight, 0) /
        dimensions.reduce((sum, d) => sum + d.weight, 0)
      return Promise.resolve({
        success: true,
        data: normalizeScoreAnalysis({
          ...data,
          id: `SA${Date.now()}`,
          totalScore: Math.round(totalScore * 100) / 100,
          dimensions,
        }),
      })
    }
    const response = await httpClient.post('/api/ai/score-analysis', data)
    return { ...response, data: normalizeScoreAnalysis(response?.data) }
  },

  async updateAnalysis(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: normalizeScoreAnalysis({ ...data, id }) })
    }
    const response = await httpClient.put(`/api/ai/score-analysis/${id}`, data)
    return { ...response, data: normalizeScoreAnalysis({ ...response?.data, ...data }) }
  },

  async deleteAnalysis(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    return httpClient.delete(`/api/ai/score-analysis/${id}`)
  },

  async compareProjects(projectIds) {
    if (isMockMode()) {
      const data = (mockData.scoreAnalyses || [])
        .filter((s) => projectIds.includes(s.projectId))
        .map(normalizeScoreAnalysis)
      return Promise.resolve({ success: true, data })
    }
    const response = await httpClient.post('/api/ai/score-analysis/compare', { projectIds })
    return {
      ...response,
      data: (response?.data || []).map(normalizeScoreAnalysis),
    }
  },
}

export default {
  scoreAnalysis: scoreAnalysisApi,
}
