// Input: tender status values from backend responses and legacy frontend UI
// Output: pure helpers for canonical tender status normalization and display
// Pos: src/views/Bidding/ - Tender status utility layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export const TENDER_STATUSES = Object.freeze({
  PENDING: 'PENDING',
  TRACKING: 'TRACKING',
  BIDDED: 'BIDDED',
  ABANDONED: 'ABANDONED'
})

const TENDER_STATUS_META = Object.freeze({
  [TENDER_STATUSES.PENDING]: {
    label: '待处理',
    tagType: 'info',
    badgeClass: 'pending'
  },
  [TENDER_STATUSES.TRACKING]: {
    label: '跟踪中',
    tagType: 'warning',
    badgeClass: 'tracking'
  },
  [TENDER_STATUSES.BIDDED]: {
    label: '已投标',
    tagType: 'success',
    badgeClass: 'bidded'
  },
  [TENDER_STATUSES.ABANDONED]: {
    label: '已放弃',
    tagType: 'danger',
    badgeClass: 'abandoned'
  }
})

const LEGACY_STATUS_ALIASES = Object.freeze({
  new: TENDER_STATUSES.PENDING,
  pending: TENDER_STATUSES.PENDING,
  contacted: TENDER_STATUSES.TRACKING,
  following: TENDER_STATUSES.TRACKING,
  quoting: TENDER_STATUSES.TRACKING,
  tracking: TENDER_STATUSES.TRACKING,
  bidding: TENDER_STATUSES.BIDDED,
  bidded: TENDER_STATUSES.BIDDED,
  abandoned: TENDER_STATUSES.ABANDONED,
  '待处理': TENDER_STATUSES.PENDING,
  '跟踪中': TENDER_STATUSES.TRACKING,
  '已投标': TENDER_STATUSES.BIDDED,
  '已放弃': TENDER_STATUSES.ABANDONED
})

export function normalizeTenderStatusCode(status) {
  if (!status) {
    return TENDER_STATUSES.PENDING
  }

  const normalizedValue = String(status).trim()
  const upperValue = normalizedValue.toUpperCase()
  if (TENDER_STATUS_META[upperValue]) {
    return upperValue
  }

  return LEGACY_STATUS_ALIASES[normalizedValue.toLowerCase()] || TENDER_STATUSES.PENDING
}

export function normalizeTenderRecord(tender = {}) {
  return {
    ...tender,
    status: normalizeTenderStatusCode(tender.status)
  }
}

export function normalizeTenderCollection(tenders = []) {
  if (!Array.isArray(tenders)) {
    return []
  }
  return tenders.map(normalizeTenderRecord)
}

export function matchesTenderStatus(actualStatus, expectedStatus) {
  if (!expectedStatus) {
    return true
  }
  return normalizeTenderStatusCode(actualStatus) === normalizeTenderStatusCode(expectedStatus)
}

export function getTenderStatusText(status) {
  if (status == null || status === '') {
    return '未知'
  }

  const normalizedValue = String(status).trim()
  const upperValue = normalizedValue.toUpperCase()
  if (TENDER_STATUS_META[upperValue]) {
    return TENDER_STATUS_META[upperValue].label
  }

  const aliasValue = LEGACY_STATUS_ALIASES[normalizedValue.toLowerCase()]
  if (aliasValue) {
    return TENDER_STATUS_META[aliasValue].label
  }

  return normalizedValue
}

export function getTenderStatusTagType(status) {
  return TENDER_STATUS_META[normalizeTenderStatusCode(status)]?.tagType || 'info'
}

export function getTenderStatusBadgeClass(status) {
  return TENDER_STATUS_META[normalizeTenderStatusCode(status)]?.badgeClass || 'pending'
}

export function toBackendTenderStatus(status) {
  return normalizeTenderStatusCode(status)
}
