// Input: backend FeeDTO, AuditLogItemDTO, project/task status strings
// Output: pure normalizer and display functions for project data transformations
// Pos: src/views/Project/ - Project module utilities
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

const FEE_TYPE_MAP = {
  BID_BOND: '保证金',
  SERVICE_FEE: '服务费',
  DOCUMENT_FEE: '标书费',
  TRAVEL_FEE: '差旅费',
  NOTARY_FEE: '公证费',
  OTHER_FEE: '其他'
}

const FEE_STATUS_MAP = {
  PENDING: 'pending',
  PAID: 'paid',
  RETURNED: 'returned',
  CANCELLED: 'cancelled'
}

const TASK_STATUS_TO_API = {
  todo: 'TODO',
  doing: 'IN_PROGRESS',
  review: 'REVIEW',
  done: 'COMPLETED',
  cancelled: 'CANCELLED'
}

const TASK_STATUS_FROM_API = {
  TODO: 'todo',
  IN_PROGRESS: 'doing',
  REVIEW: 'review',
  COMPLETED: 'done',
  CANCELLED: 'cancelled'
}

const PROJECT_STATUS_TEXT = {
  INITIATED: '已立项',
  PREPARING: '编制中',
  REVIEWING: '评审中',
  SEALING: '盖章中',
  BIDDING: '投标中',
  ARCHIVED: '已归档',
  drafting: '草稿中',
  reviewing: '评审中',
  bidding: '投标中',
  won: '已中标',
  lost: '未中标',
  pending: '待立项'
}

const PROJECT_STATUS_TYPE = {
  INITIATED: 'info',
  PREPARING: 'primary',
  REVIEWING: 'warning',
  SEALING: 'warning',
  BIDDING: 'primary',
  ARCHIVED: 'success',
  drafting: 'info',
  reviewing: 'warning',
  bidding: 'primary',
  won: 'success',
  lost: 'danger',
  pending: 'info'
}

const ACTION_TYPE_LABEL = {
  create: '创建',
  update: '更新',
  delete: '删除',
  query: '查询',
  login: '登录',
  logout: '登出',
  approve: '审批通过',
  reject: '审批驳回',
  submit: '提交',
  export: '导出',
  import: '导入'
}

/**
 * Backend FeeDTO → frontend display shape
 */
export function normalizeFeeForDisplay(backendFee) {
  if (backendFee == null) {
    return { id: null, type: '其他', amount: 0, status: 'pending', date: '', remark: '' }
  }

  const rawAmount = backendFee.amount != null ? Number(backendFee.amount) : 0
  const amount = Number.isNaN(rawAmount) ? 0 : rawAmount

  const feeDate = backendFee.feeDate
  const date = typeof feeDate === 'string' && feeDate.length > 0
    ? feeDate.slice(0, 10)
    : ''

  return {
    id: backendFee.id !== undefined ? backendFee.id : null,
    type: FEE_TYPE_MAP[backendFee.feeType] || '其他',
    amount,
    status: FEE_STATUS_MAP[backendFee.status] || 'pending',
    date,
    remark: backendFee.remarks || ''
  }
}

/**
 * Backend AuditLogItemDTO → frontend activity timeline shape
 */
export function normalizeAuditLogForTimeline(auditLog) {
  if (auditLog == null) {
    return { id: null, user: '未知用户', action: '', time: '' }
  }

  const actionTypeLabel = auditLog.actionType
    ? (ACTION_TYPE_LABEL[String(auditLog.actionType).toLowerCase()] || auditLog.actionType)
    : ''

  return {
    id: auditLog.id !== undefined ? auditLog.id : null,
    user: auditLog.operator || '未知用户',
    action: auditLog.detail || actionTypeLabel || '',
    time: auditLog.time || ''
  }
}

export function getProjectStatusText(status) {
  if (status == null || status === '') return ''
  return PROJECT_STATUS_TEXT[status] || status
}

export function getProjectStatusType(status) {
  return PROJECT_STATUS_TYPE[status] || 'info'
}

/**
 * Frontend task status → backend enum
 */
export function normalizeTaskStatusForApi(frontendStatus) {
  if (frontendStatus == null) return undefined
  return TASK_STATUS_TO_API[frontendStatus] || frontendStatus
}

/**
 * Backend task status enum → frontend status
 */
export function normalizeTaskStatusFromApi(backendStatus) {
  if (backendStatus == null) return undefined
  return TASK_STATUS_FROM_API[backendStatus] || backendStatus
}
