// Input: bidding list records, forms, users, and browser storage payloads
// Output: pure helpers for permissions, safe URLs, payloads, and display values
// Pos: src/views/Bidding/list/ - Pure helper layer for the bidding list page
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { DEFAULT_SOURCE_CONFIG } from './constants.js'

export function normalizeRole(value) {
  return String(value || '').trim().toLowerCase().replace(/^role_/, '')
}

export function resolveUserRole(userStore) {
  return normalizeRole(
    userStore?.currentUser?.roleCode
      || userStore?.currentUser?.role
      || userStore?.userRole
      || 'staff',
  )
}

export function buildPermissionFlags(role) {
  const normalized = normalizeRole(role)
  const canManageTenders = normalized === 'admin' || normalized === 'manager'
  return {
    canManageTenders,
    canCreateTender: canManageTenders,
    canDeleteTenders: normalized === 'admin',
    canSyncExternalSource: normalized === 'admin',
  }
}

export function sanitizeSourceConfigForStorage(config = {}) {
  const merged = { ...DEFAULT_SOURCE_CONFIG, ...config }
  const { apiKey, ...safeConfig } = merged
  return {
    ...safeConfig,
    platforms: Array.isArray(safeConfig.platforms) ? safeConfig.platforms : [],
    keywords: Array.isArray(safeConfig.keywords) ? safeConfig.keywords : [],
    regions: Array.isArray(safeConfig.regions) ? safeConfig.regions : [],
  }
}

export function restoreSourceConfig(rawValue, storageWriter) {
  if (!rawValue) {
    return { ...DEFAULT_SOURCE_CONFIG }
  }

  try {
    const parsed = JSON.parse(rawValue)
    const safeConfig = sanitizeSourceConfigForStorage(parsed)
    if (Object.prototype.hasOwnProperty.call(parsed, 'apiKey') && storageWriter) {
      storageWriter(JSON.stringify(safeConfig))
    }
    return { ...DEFAULT_SOURCE_CONFIG, ...safeConfig, apiKey: '' }
  } catch {
    return { ...DEFAULT_SOURCE_CONFIG }
  }
}

export function safeTenderUrl(value) {
  if (!value) {
    return ''
  }

  try {
    const url = new URL(String(value))
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : ''
  } catch {
    return ''
  }
}

export function formatLocalDateTime(value) {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return null
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function formatLocalDate(value = new Date()) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return null
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function buildManualTenderPayload(form = {}) {
  return {
    title: form.title,
    budget: form.budget,
    region: form.region,
    industry: form.industry,
    deadline: formatLocalDateTime(form.deadline),
    publishDate: formatLocalDate(),
    source: 'manual',
    purchaserName: form.purchaser,
    contactName: form.contact,
    contactPhone: form.phone,
    description: form.description,
    tags: Array.isArray(form.tags) ? form.tags : [],
    status: 'PENDING',
  }
}

export function getScoreClass(score) {
  if (Number(score) >= 90) return 'score-excellent'
  if (Number(score) >= 80) return 'score-good'
  return 'score-normal'
}

export function getScoreTagType(score) {
  if (Number(score) >= 90) return 'success'
  if (Number(score) >= 80) return 'warning'
  return 'info'
}

export function getSourceTagType(source) {
  const map = {
    internal: 'info',
    external: 'success',
    manual: 'warning',
    CEB: 'success',
    中国招标投标公共服务平台: 'success',
  }
  return map[source] || 'info'
}

export function getSourceText(source) {
  const map = {
    internal: '内部',
    external: '外部获取',
    manual: '人工录入',
    CEB: '公共平台(CEB)',
    中国招标投标公共服务平台: '公共平台(CEB)',
  }
  return map[source] || source || '未知'
}

export function normalizeAssignmentCandidate(candidate = {}) {
  return {
    id: Number(candidate.id),
    name: candidate.name || `用户#${candidate.id}`,
    departmentName: candidate.departmentName || '未分组',
    roleCode: candidate.roleCode || '',
  }
}

export function buildDistributionPreview({ tenders = [], candidates = [], form = {} } = {}) {
  const usableCandidates = candidates.filter((item) => Number.isFinite(Number(item.id)))
  if (tenders.length === 0 || usableCandidates.length === 0) return []
  const targets = form.type === 'manual' && form.assignees?.length
    ? usableCandidates.filter((item) => form.assignees.includes(item.id))
    : usableCandidates
  if (targets.length === 0) return []

  const groups = new Map(targets.map((candidate) => [candidate.id, { ...candidate, tenders: [] }]))
  tenders.forEach((tender, index) => {
    const target = targets[index % targets.length]
    groups.get(target.id)?.tenders.push(tender)
  })

  return [...groups.values()]
    .filter((item) => item.tenders.length > 0)
    .map((item) => ({ ...item, count: item.tenders.length }))
}

function firstFiniteNumber(...values) {
  for (const value of values) {
    const numericValue = Number(value)
    if (Number.isFinite(numericValue)) {
      return numericValue
    }
  }
  return 0
}

export function summarizeExternalSyncResult(response = {}) {
  const data = response?.data || {}
  return {
    visible: true,
    saved: firstFiniteNumber(data.saved, data.savedCount, response.saved, response.savedCount),
    skipped: firstFiniteNumber(data.skipped, data.skippedCount, response.skipped, response.skippedCount),
    message: data.message || response?.message || '标讯同步完成',
  }
}
