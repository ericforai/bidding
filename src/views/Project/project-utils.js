// Input: backend FeeDTO, AuditLogItemDTO, task status strings
// Output: pure normalizer functions for project data transformations
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
  done: 'COMPLETED',
  review: 'IN_REVIEW'
}

const TASK_STATUS_FROM_API = {
  TODO: 'todo',
  IN_PROGRESS: 'doing',
  COMPLETED: 'done',
  IN_REVIEW: 'review'
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
