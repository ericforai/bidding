// Input: backend response objects, frontend form data
// Output: pure normalizer functions for bidding data transformations
// Pos: src/views/Bidding/ - Bidding module utilities
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import {
  getTenderStatusText,
  toBackendTenderStatus,
} from './bidding-utils-status.js'

/**
 * Backend status enum → Chinese display text
 */
export function normalizeTenderStatus(backendStatus) {
  return getTenderStatusText(backendStatus) || backendStatus || '未知'
}

/**
 * Frontend form data → TenderRequest for POST /api/tenders
 */
export function normalizeTenderForCreate(formData) {
  return {
    title: formData.title || '',
    source: formData.source || '人工录入',
    budget: formData.budget != null ? Number(formData.budget) : 0,
    deadline: formData.deadline || null,
    status: 'PENDING',
    aiScore: formData.aiScore != null ? Number(formData.aiScore) : 0,
    riskLevel: formData.riskLevel || null,
    originalUrl: formData.originalUrl || '',
    externalId: formData.externalId || ''
  }
}

/**
 * Build partial update payload for PUT /api/tenders/{id}
 * Only includes fields that are present in changes
 */
export function buildTenderUpdatePayload(changes) {
  const payload = {}
  if (changes.status !== undefined) payload.status = changes.status
  if (changes.title !== undefined) payload.title = changes.title
  if (changes.budget !== undefined) payload.budget = Number(changes.budget)
  if (changes.deadline !== undefined) payload.deadline = changes.deadline
  if (changes.aiScore !== undefined) payload.aiScore = Number(changes.aiScore)
  if (changes.riskLevel !== undefined) payload.riskLevel = changes.riskLevel
  if (changes.originalUrl !== undefined) payload.originalUrl = changes.originalUrl
  return payload
}

export function safeTenderUrl(value) {
  if (!value) return ''
  try {
    const url = new URL(String(value))
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : ''
  } catch {
    return ''
  }
}

export function formatBudgetWan(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return '0'
  const wan = number / 10000
  const maximumFractionDigits = Math.abs(wan) >= 100 ? 0 : 2
  return wan.toLocaleString('zh-CN', { maximumFractionDigits })
}

/**
 * BatchOperationResponse → user-facing summary message
 */
export function normalizeBatchResult(response) {
  if (!response) return { ok: false, message: '操作失败：无响应' }

  const data = response.data || response
  const success = data.success !== false
  const total = data.totalCount || 0
  const succeeded = data.successCount || 0
  const failed = data.failureCount || 0

  if (success && failed === 0) {
    return { ok: true, message: `操作成功，共处理 ${succeeded} 条` }
  }
  if (succeeded > 0 && failed > 0) {
    const errors = (data.errors || []).map(e => e.errorMessage).join('; ')
    return { ok: false, message: `部分成功：${succeeded} 条成功，${failed} 条失败。${errors}` }
  }
  const errors = (data.errors || []).map(e => e.errorMessage).join('; ')
  return { ok: false, message: `操作失败：${errors || '未知错误'}` }
}

/**
 * AI analysis dimensionScores from API → chart-friendly format
 * Input: [{name: "技术匹配", score: 85}, ...]
 * Output: [{name: "技术匹配", score: 85, percentage: "85%", level: "high"}, ...]
 */
export function normalizeAiDimensions(dimensionScores) {
  if (!Array.isArray(dimensionScores)) return []
  return dimensionScores.map(d => ({
    name: d.name || '未知维度',
    score: d.score != null ? Number(d.score) : 0,
    percentage: `${d.score != null ? d.score : 0}%`,
    level: d.score >= 80 ? 'high' : d.score >= 60 ? 'medium' : 'low',
    description: d.description || '',
    suggestion: d.suggestion || ''
  }))
}

/**
 * AI analysis risks from API → frontend display format
 * Input: [{level: "HIGH", desc: "...", action: "..."}, ...]
 */
export function normalizeAiRisks(risks) {
  if (!Array.isArray(risks)) return []
  const levelMap = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return risks.map(r => ({
    level: r.level || 'LOW',
    type: levelMap[r.level] || 'info',
    description: r.desc || '',
    action: r.action || ''
  }))
}

/**
 * Frontend status (lowercase) → Backend enum (uppercase)
 */
export function toBackendStatus(frontendStatus) {
  if (frontendStatus == null) {
    return frontendStatus
  }

  const normalized = toBackendTenderStatus(frontendStatus)
  const rawValue = String(frontendStatus).trim()
  return normalized === 'PENDING' && rawValue.toUpperCase() !== 'PENDING'
    && rawValue.toLowerCase() !== 'new'
    && rawValue.toLowerCase() !== 'pending'
    && rawValue !== '待处理'
    ? frontendStatus
    : normalized
}
