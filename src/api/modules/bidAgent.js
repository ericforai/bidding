// Input: httpClient and project bid-agent endpoints
// Output: bidAgentApi - create/status/apply/review accessors for bid writing runs
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'

const toArray = (value) => (Array.isArray(value) ? value : [])

function normalizeConfidence(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return null
  return numeric > 1 ? Math.round(numeric) : Math.round(numeric * 100)
}

function normalizeSection(section = {}) {
  const metadata = section.metadata || {}
  return {
    ...section,
    id: section.id ?? section.sectionId ?? section.key,
    title: section.title || section.name || section.heading || '未命名章节',
    content: section.content || section.text || section.body || section.summary || '',
    source: section.source || section.sourceName || section.sourceTitle || metadata.source || '',
    confidence: normalizeConfidence(section.confidence ?? metadata.confidence),
  }
}

function normalizeStage(stage = {}) {
  return {
    ...stage,
    key: stage.key || stage.code || stage.name || stage.stage,
    title: stage.title || stage.name || stage.label || stage.stage || '处理阶段',
    status: stage.status || stage.state || 'PENDING',
    message: stage.message || stage.description || '',
  }
}

export function normalizeBidAgentRun(data = null) {
  if (!data) return null
  const draft = data.draft || data.draftResult || {}
  const sections = toArray(draft.sections).length ? draft.sections : (data.draftSections || data.sections)

  return {
    ...data,
    id: data.id ?? data.runId,
    runId: data.runId ?? data.id,
    status: data.status || data.state || 'UNKNOWN',
    stages: toArray(data.stages || data.stageStatuses).map(normalizeStage),
    draft: {
      ...draft,
      sections: toArray(sections).map(normalizeSection),
    },
    gaps: toArray(data.gaps || data.requirementGaps),
    risks: toArray(data.risks || data.riskItems),
    manualConfirmations: toArray(data.manualConfirmations || data.confirmations),
  }
}

export const bidAgentApi = {
  async createRun(projectId, payload = {}) {
    const response = await httpClient.post(`/api/projects/${projectId}/bid-agent/runs`, payload)
    return { ...response, data: normalizeBidAgentRun(response?.data) }
  },

  async getRun(projectId, runId) {
    const response = await httpClient.get(`/api/projects/${projectId}/bid-agent/runs/${runId}`)
    return { ...response, data: normalizeBidAgentRun(response?.data) }
  },

  async applyRun(projectId, runId, payload = {}) {
    return httpClient.post(`/api/projects/${projectId}/bid-agent/runs/${runId}/apply`, payload)
  },

  async createReview(projectId, payload = {}) {
    return httpClient.post(`/api/projects/${projectId}/bid-agent/reviews`, payload)
  },
}

export default bidAgentApi
