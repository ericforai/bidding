/**
 * AI 智能分析模块 API
 * 支持双模式切换，并在 API 模式下对齐当前后端契约
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'
import { buildFeatureUnavailableResponse } from '../featureAvailability.js'

function isNumericId(id) {
  return /^\d+$/.test(String(id))
}

function invalidIdMessage(entityName) {
  return buildFeatureUnavailableResponse({
    feature: `${entityName}-numeric-id`,
    title: '当前 ID 格式暂未接入',
    message: `Current backend only supports numeric ${entityName} IDs in API mode`,
    hint: '请使用真实后端返回的数字 ID 访问该资源。',
    scope: 'action',
  })
}

function normalizeBidAnalysis(data = {}) {
  return {
    tenderId: data?.tenderId,
    winScore: Number(data?.winScore || 0),
    suggestion: data?.suggestion || data?.summary || '暂无分析建议',
    dimensionScores: Array.isArray(data?.dimensionScores) ? data.dimensionScores : [],
    risks: Array.isArray(data?.risks) ? data.risks : [],
    autoTasks: Array.isArray(data?.autoTasks) ? data.autoTasks : [],
  }
}

function normalizeScoreAnalysis(data = {}) {
  return {
    id: data?.id,
    projectId: data?.projectId,
    overallScore: Number(data?.overallScore || 0),
    riskLevel: data?.riskLevel || 'UNKNOWN',
    summary: data?.summary || '',
    dimensions: Array.isArray(data?.dimensions)
      ? data.dimensions.map((dimension) => ({
          id: dimension?.id,
          name: dimension?.dimensionName || '未命名维度',
          score: Number(dimension?.score || 0),
          weight: Number(dimension?.weight || 0),
          comments: dimension?.comments || '',
        }))
      : [],
  }
}

function buildScorePreview(context = {}) {
  const base = mockData.scoreAnalysis?.P001 || {}
  const budget = Number(context?.budget || 0)
  const industry = context?.industry || ''
  const tags = Array.isArray(context?.tags) ? context.tags : []

  let winScore = 60
  if (industry === '政府') winScore += 10
  if (industry === '央国企') winScore += 5
  if (tags.includes('信创')) winScore += 5
  if (tags.includes('智慧城市')) winScore += 5
  if (budget > 500) winScore -= 5

  const boundedWinScore = Math.max(0, Math.min(100, winScore))
  const winLevel = boundedWinScore >= 80 ? 'high' : boundedWinScore >= 60 ? 'medium' : 'low'

  const scoreCategories = Array.isArray(base.scoreCategories)
    ? base.scoreCategories.map((category) => ({ ...category }))
    : []
  const gapItems = Array.isArray(base.gapItems) ? base.gapItems.map((item) => ({ ...item })) : []

  if (tags.includes('信创')) {
    const techCategory = scoreCategories.find((item) => item.name === '技术')
    if (techCategory) {
      techCategory.covered = Math.min(techCategory.total, techCategory.covered + 4)
      techCategory.percentage = Math.round((techCategory.covered / techCategory.total) * 100)
      techCategory.gaps = techCategory.gaps.filter((gap) => gap !== '大数据平台')
    }
  }

  const risks = [
    ...(base.aiSummary?.risks || []),
    ...(budget > 500 ? [{ level: 'medium', content: '预算较高，需重点关注报价竞争力' }] : []),
  ]

  const suggestions = [
    ...(base.aiSummary?.suggestions || []),
    ...(tags.includes('信创') ? ['突出信创兼容能力，补充国产化生态证明材料'] : []),
  ]

  const generatedTasks = gapItems.map((item, index) => ({
    name: `补齐${item.scorePoint}`,
    priority: item.category === '技术' ? 'high' : 'medium',
    suggestion: item.required,
    selected: index < 4,
  }))

  return {
    aiSummary: {
      winScore: boundedWinScore,
      winLevel,
      risks,
      suggestions,
    },
    scoreAnalysis: {
      scoreCategories,
      gapItems,
    },
    generatedTasks,
  }
}

function normalizeCompetitionAnalysis(data) {
  const list = Array.isArray(data) ? data : data ? [data] : []
  return list.map((item) => ({
    id: item?.id,
    projectId: item?.projectId,
    competitorId: item?.competitorId,
    winProbability: Number(item?.winProbability || 0),
    competitiveAdvantage: item?.competitiveAdvantage || '',
    recommendedStrategy: item?.recommendedStrategy || '',
    riskFactors: item?.riskFactors || '',
    analysisDate: item?.analysisDate || '',
  }))
}

function normalizeRoiAnalysis(data = {}) {
  const estimatedRevenue = Number(data?.estimatedRevenue || 0)
  const estimatedCost = Number(data?.estimatedCost || 0)
  const estimatedProfit = Number(data?.estimatedProfit || estimatedRevenue - estimatedCost)
  return {
    id: data?.id,
    projectId: data?.projectId,
    estimatedRevenue,
    estimatedCost,
    estimatedProfit,
    roiPercentage: Number(data?.roiPercentage || 0),
    paybackPeriodMonths: Number(data?.paybackPeriodMonths || 0),
    riskFactors: data?.riskFactors || '',
    assumptions: data?.assumptions || '',
  }
}

function normalizeComplianceResult(data = {}) {
  return {
    id: data?.id,
    projectId: data?.projectId,
    tenderId: data?.tenderId,
    overallStatus: data?.overallStatus || 'UNKNOWN',
    overallScore: Number(data?.riskScore || data?.overallScore || 0),
    issues: Array.isArray(data?.issues) ? data.issues : [],
    checkedAt: data?.checkedAt || '',
    checkedBy: data?.checkedBy || '',
  }
}

function normalizeProjectAiCards(data = {}) {
  return {
    score: data?.score
      ? {
          overallScore: Number(data.score.overallScore || 0),
          riskLevel: data.score.riskLevel || 'UNKNOWN',
          summary: data.score.summary || '',
          dimensions: Array.isArray(data.score.dimensions) ? data.score.dimensions : [],
        }
      : null,
    competition: Array.isArray(data?.competition) ? data.competition : [],
    compliance: Array.isArray(data?.compliance)
      ? data.compliance.map(normalizeComplianceResult)
      : [],
    roi: data?.roi ? normalizeRoiAnalysis(data.roi) : null,
  }
}

export const bidAnalysisApi = {
  async getAnalysis(tenderId) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeBidAnalysis(mockData.aiAnalysis?.[tenderId] || mockData.aiAnalysis?.B001 || mockData.aiAnalysis?.T001),
      })
    }
    if (!isNumericId(tenderId)) return Promise.resolve(invalidIdMessage('tender'))

    const response = await httpClient.post(`/api/tenders/${tenderId}/ai-analysis`)
    return { ...response, data: normalizeBidAnalysis(response?.data) }
  },
}

export const scoreAnalysisApi = {
  async getAnalysis(projectId) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.scoreAnalysis?.[projectId] || mockData.scoreAnalysis?.P001 || null,
      })
    }
    if (!isNumericId(projectId)) return Promise.resolve(invalidIdMessage('project'))

    const response = await httpClient.get(`/api/ai/score-analysis/project/${projectId}`)
    return { ...response, data: normalizeScoreAnalysis(response?.data) }
  },

  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { ...data, id: `SA${Date.now()}` } })
    }
    return httpClient.post('/api/ai/score-analysis', data)
  },

  async compare(id1, id2) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          project1: mockData.scoreAnalysis?.[id1] || mockData.scoreAnalysis?.P001,
          project2: mockData.scoreAnalysis?.[id2] || mockData.scoreAnalysis?.P001,
        },
      })
    }
    if (!isNumericId(id1) || !isNumericId(id2)) return Promise.resolve(invalidIdMessage('project'))

    const response = await httpClient.get(`/api/ai/score-analysis/compare/${id1}/${id2}`)
    const list = Array.isArray(response?.data) ? response.data.map(normalizeScoreAnalysis) : []
    return {
      ...response,
      data: {
        project1: list[0] || null,
        project2: list[1] || null,
      },
    }
  },

  async generatePreview(context) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: buildScorePreview(context),
      })
    }

    const response = await httpClient.post('/api/projects/score-preview', {
      projectId: context?.projectId || null,
      tenderId: context?.tenderId || null,
      projectName: context?.projectName || context?.name || '',
      industry: context?.industry || '',
      budget: Number(context?.budget || 0),
      tags: Array.isArray(context?.tags) ? context.tags : [],
    })
    if (!response?.data) {
      return buildFeatureUnavailableResponse({
        feature: 'project-score-preview',
        title: '评分预览暂未接入',
        message: 'Score preview endpoint returned no preview payload',
        hint: '你仍然可以继续创建项目，并在后续环节手动补充评分分析。',
        scope: 'section',
      })
    }
    return { ...response, data: response.data }
  },
}

export const projectAiApi = {
  async getCards(projectId) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          score: normalizeScoreAnalysis(mockData.scoreAnalysis?.[projectId] || mockData.scoreAnalysis?.P001 || {}),
          competition: mockData.competitionIntel?.[projectId]?.competitors || mockData.competitionIntel?.P001?.competitors || [],
          compliance: [mockData.complianceCheck?.[projectId] || mockData.complianceCheck?.P001].filter(Boolean),
          roi: normalizeRoiAnalysis(mockData.roiAnalysis?.[projectId] || mockData.roiAnalysis?.P001 || {}),
        },
      })
    }
    if (!isNumericId(projectId)) return Promise.resolve(invalidIdMessage('project'))

    const response = await httpClient.get(`/api/projects/${projectId}/ai-cards`)
    return { ...response, data: normalizeProjectAiCards(response?.data) }
  },
}

export const competitionApi = {
  async getProjectAnalysis(projectId) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.competitionIntel?.[projectId] || mockData.competitionIntel?.P001 || null,
      })
    }
    if (!isNumericId(projectId)) return Promise.resolve(invalidIdMessage('project'))

    const response = await httpClient.get(`/api/ai/competition/project/${projectId}`)
    return {
      ...response,
      data: normalizeCompetitionAnalysis(response?.data),
    }
  },

  async getCompetitors() {
    if (isMockMode()) {
      const analysis = mockData.competitionIntel?.P001
      return Promise.resolve({ success: true, data: analysis?.competitors || [] })
    }
    return httpClient.get('/api/ai/competition/competitors')
  },

  async addCompetitor(data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { ...data, id: `C${Date.now()}` } })
    }
    return httpClient.post('/api/ai/competition/competitors', data)
  },

  async analyze(projectId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: mockData.competitionIntel?.P001 || null })
    }
    if (!isNumericId(projectId)) return Promise.resolve(invalidIdMessage('project'))

    const response = await httpClient.post(`/api/ai/competition/project/${projectId}/analyze`)
    return { ...response, data: normalizeCompetitionAnalysis(response?.data)[0] || null }
  },
}

export const roiApi = {
  async getAnalysis(projectId) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.roiAnalysis?.[projectId] || mockData.roiAnalysis?.P001 || null,
      })
    }
    if (!isNumericId(projectId)) return Promise.resolve(invalidIdMessage('project'))

    const response = await httpClient.get(`/api/ai/roi/project/${projectId}`)
    return { ...response, data: normalizeRoiAnalysis(response?.data) }
  },

  async calculate(data) {
    if (isMockMode()) {
      const cost = Object.values(data.estimatedCost || {}).reduce((sum, value) => sum + (value || 0), 0)
      const revenue = Number(data.projectBudget || 0)
      const profit = revenue - cost
      return Promise.resolve({
        success: true,
        data: {
          ...data,
          totalCost: cost,
          netProfit: profit,
          profitMargin: revenue ? Number(((profit / revenue) * 100).toFixed(1)) : 0,
        },
      })
    }
    if (!isNumericId(data?.projectId)) return Promise.resolve(invalidIdMessage('project'))

    return httpClient.post(`/api/ai/roi/project/${data.projectId}/calculate`, data)
  },

  async sensitivity(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          scenarios: [
            { name: '乐观', roi: 85, probability: 0.3 },
            { name: '中性', roi: 72, probability: 0.5 },
            { name: '悲观', roi: 45, probability: 0.2 },
          ],
        },
      })
    }
    return httpClient.post('/api/ai/roi/sensitivity', data)
  },
}

export const complianceApi = {
  async getCheckResult(projectId) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.complianceCheck?.[projectId] || mockData.complianceCheck?.P001 || null,
      })
    }
    if (!isNumericId(projectId)) return Promise.resolve(invalidIdMessage('project'))

    const response = await httpClient.get(`/api/compliance/project/${projectId}/results`)
    const results = Array.isArray(response?.data) ? response.data.map(normalizeComplianceResult) : []
    return { ...response, data: results }
  },

  async performCheck(projectId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: mockData.complianceCheck?.P001 || null })
    }
    if (!isNumericId(projectId)) return Promise.resolve(invalidIdMessage('project'))

    const response = await httpClient.post(`/api/compliance/check/project/${projectId}`)
    return { ...response, data: normalizeComplianceResult(response?.data) }
  },

  async performTenderCheck(tenderId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: mockData.complianceCheck?.P001 || null })
    }
    if (!isNumericId(tenderId)) return Promise.resolve(invalidIdMessage('tender'))

    const response = await httpClient.post(`/api/compliance/check/tender/${tenderId}`)
    return { ...response, data: normalizeComplianceResult(response?.data) }
  },
}

export default {
  bid: bidAnalysisApi,
  score: scoreAnalysisApi,
  project: projectAiApi,
  competition: competitionApi,
  roi: roiApi,
  compliance: complianceApi,
}
