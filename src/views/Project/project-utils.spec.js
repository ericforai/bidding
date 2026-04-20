import { describe, it, expect } from 'vitest'
import {
  normalizeFeeForDisplay,
  normalizeAuditLogForTimeline,
  normalizeTaskStatusForApi,
  normalizeTaskStatusFromApi
} from './project-utils.js'

describe('normalizeFeeForDisplay', () => {
  it('normalizes a full backend FeeDTO', () => {
    const input = {
      id: 42,
      projectId: 7,
      feeType: 'BID_BOND',
      amount: 50000,
      status: 'PAID',
      feeDate: '2026-04-10T08:30:00',
      remarks: '已确认到账',
      createdAt: '2026-04-01T10:00:00'
    }
    expect(normalizeFeeForDisplay(input)).toEqual({
      id: 42,
      type: '保证金',
      amount: 50000,
      status: 'paid',
      date: '2026-04-10',
      remark: '已确认到账'
    })
  })

  it.each([
    ['BID_BOND', '保证金'],
    ['SERVICE_FEE', '服务费'],
    ['DOCUMENT_FEE', '标书费'],
    ['TRAVEL_FEE', '差旅费'],
    ['NOTARY_FEE', '公证费'],
    ['OTHER_FEE', '其他']
  ])('maps feeType "%s" to "%s"', (feeType, expected) => {
    const result = normalizeFeeForDisplay({ feeType })
    expect(result.type).toBe(expected)
  })

  it('maps unknown feeType to "其他"', () => {
    expect(normalizeFeeForDisplay({ feeType: 'UNKNOWN_TYPE' }).type).toBe('其他')
  })

  it('maps missing feeType to "其他"', () => {
    expect(normalizeFeeForDisplay({}).type).toBe('其他')
  })

  it.each([
    ['PENDING', 'pending'],
    ['PAID', 'paid'],
    ['RETURNED', 'returned'],
    ['CANCELLED', 'cancelled']
  ])('maps status "%s" to "%s"', (status, expected) => {
    const result = normalizeFeeForDisplay({ status })
    expect(result.status).toBe(expected)
  })

  it('maps unknown status to "pending"', () => {
    expect(normalizeFeeForDisplay({ status: 'UNKNOWN' }).status).toBe('pending')
  })

  it('maps missing status to "pending"', () => {
    expect(normalizeFeeForDisplay({}).status).toBe('pending')
  })

  it('coerces string amount to number', () => {
    expect(normalizeFeeForDisplay({ amount: '12345' }).amount).toBe(12345)
  })

  it('defaults null amount to 0', () => {
    expect(normalizeFeeForDisplay({ amount: null }).amount).toBe(0)
  })

  it('defaults undefined amount to 0', () => {
    expect(normalizeFeeForDisplay({}).amount).toBe(0)
  })

  it('handles NaN amount as 0', () => {
    expect(normalizeFeeForDisplay({ amount: 'not-a-number' }).amount).toBe(0)
  })

  it('slices feeDate string to first 10 chars for date', () => {
    expect(normalizeFeeForDisplay({ feeDate: '2026-04-10T08:30:00' }).date).toBe('2026-04-10')
  })

  it('handles date-only feeDate', () => {
    expect(normalizeFeeForDisplay({ feeDate: '2026-04-10' }).date).toBe('2026-04-10')
  })

  it('defaults missing feeDate to empty string', () => {
    expect(normalizeFeeForDisplay({}).date).toBe('')
  })

  it('defaults null feeDate to empty string', () => {
    expect(normalizeFeeForDisplay({ feeDate: null }).date).toBe('')
  })

  it('uses remarks field for remark output', () => {
    expect(normalizeFeeForDisplay({ remarks: '备注内容' }).remark).toBe('备注内容')
  })

  it('defaults missing remarks to empty string', () => {
    expect(normalizeFeeForDisplay({}).remark).toBe('')
  })

  it('returns safe defaults for null input', () => {
    expect(normalizeFeeForDisplay(null)).toEqual({
      id: null,
      type: '其他',
      amount: 0,
      status: 'pending',
      date: '',
      remark: ''
    })
  })

  it('returns safe defaults for undefined input', () => {
    expect(normalizeFeeForDisplay(undefined)).toEqual({
      id: null,
      type: '其他',
      amount: 0,
      status: 'pending',
      date: '',
      remark: ''
    })
  })
})

describe('normalizeAuditLogForTimeline', () => {
  it('normalizes a full audit log entry', () => {
    const input = {
      id: 101,
      time: '2026-04-10 14:30:00',
      operator: '张三',
      actionType: 'UPDATE',
      module: 'project',
      target: '项目A',
      detail: '修改了项目状态为进行中',
      status: 'SUCCESS'
    }
    expect(normalizeAuditLogForTimeline(input)).toEqual({
      id: 101,
      user: '张三',
      action: '修改了项目状态为进行中',
      time: '2026-04-10 14:30:00'
    })
  })

  it('uses operator for user field', () => {
    expect(normalizeAuditLogForTimeline({ operator: '李四' }).user).toBe('李四')
  })

  it('falls back to "未知用户" when operator is missing', () => {
    expect(normalizeAuditLogForTimeline({}).user).toBe('未知用户')
  })

  it('falls back to "未知用户" when operator is null', () => {
    expect(normalizeAuditLogForTimeline({ operator: null }).user).toBe('未知用户')
  })

  it('falls back to "未知用户" when operator is empty string', () => {
    expect(normalizeAuditLogForTimeline({ operator: '' }).user).toBe('未知用户')
  })

  it('prefers detail over actionType for action', () => {
    const input = { detail: '详细描述', actionType: 'CREATE' }
    expect(normalizeAuditLogForTimeline(input).action).toBe('详细描述')
  })

  it('falls back to localized actionType when detail is missing', () => {
    expect(normalizeAuditLogForTimeline({ actionType: 'delete' }).action).toBe('删除')
    expect(normalizeAuditLogForTimeline({ actionType: 'create' }).action).toBe('创建')
    expect(normalizeAuditLogForTimeline({ actionType: 'update' }).action).toBe('更新')
    expect(normalizeAuditLogForTimeline({ actionType: 'DELETE' }).action).toBe('删除')
  })

  it('passes through unknown actionType as-is', () => {
    expect(normalizeAuditLogForTimeline({ actionType: 'CUSTOM_ACTION' }).action).toBe('CUSTOM_ACTION')
  })

  it('falls back to empty string when both detail and actionType are missing', () => {
    expect(normalizeAuditLogForTimeline({}).action).toBe('')
  })

  it('keeps time as-is', () => {
    expect(normalizeAuditLogForTimeline({ time: '2026-04-10 14:30:00' }).time).toBe('2026-04-10 14:30:00')
  })

  it('defaults missing time to empty string', () => {
    expect(normalizeAuditLogForTimeline({}).time).toBe('')
  })

  it('returns safe defaults for null input', () => {
    expect(normalizeAuditLogForTimeline(null)).toEqual({
      id: null,
      user: '未知用户',
      action: '',
      time: ''
    })
  })

  it('returns safe defaults for undefined input', () => {
    expect(normalizeAuditLogForTimeline(undefined)).toEqual({
      id: null,
      user: '未知用户',
      action: '',
      time: ''
    })
  })
})

describe('normalizeTaskStatusForApi', () => {
  it.each([
    ['todo', 'TODO'],
    ['doing', 'IN_PROGRESS'],
    ['done', 'COMPLETED'],
    ['review', 'REVIEW']
  ])('maps frontend status "%s" to backend "%s"', (input, expected) => {
    expect(normalizeTaskStatusForApi(input)).toBe(expected)
  })

  it('passes through unknown values unchanged', () => {
    expect(normalizeTaskStatusForApi('CUSTOM_STATUS')).toBe('CUSTOM_STATUS')
  })

  it('returns undefined for null', () => {
    expect(normalizeTaskStatusForApi(null)).toBeUndefined()
  })

  it('returns undefined for undefined', () => {
    expect(normalizeTaskStatusForApi(undefined)).toBeUndefined()
  })
})

describe('normalizeTaskStatusFromApi', () => {
  it.each([
    ['TODO', 'todo'],
    ['IN_PROGRESS', 'doing'],
    ['COMPLETED', 'done'],
    ['REVIEW', 'review']
  ])('maps backend status "%s" to frontend "%s"', (input, expected) => {
    expect(normalizeTaskStatusFromApi(input)).toBe(expected)
  })

  it('passes through unknown values unchanged', () => {
    expect(normalizeTaskStatusFromApi('CUSTOM_STATUS')).toBe('CUSTOM_STATUS')
  })

  it('returns undefined for null', () => {
    expect(normalizeTaskStatusFromApi(null)).toBeUndefined()
  })

  it('returns undefined for undefined', () => {
    expect(normalizeTaskStatusFromApi(undefined)).toBeUndefined()
  })
})
