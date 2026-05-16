import { describe, expect, it, vi } from 'vitest'
import {
  buildManualTenderPayload,
  buildPermissionFlags,
  formatBudgetWan,
  getSourceTypeTagType,
  getSourceTypeText,
  normalizeManualTenderParseResult,
  restoreSourceConfig,
  safeTenderUrl,
  sanitizeSourceConfigForStorage,
  summarizeExternalSyncResult,
} from './helpers.js'

describe('bidding list helpers', () => {
  it('strips apiKey before source config persistence', () => {
    const result = sanitizeSourceConfigForStorage({
      platforms: ['中国政府采购网'],
      apiKey: 'secret-token',
      keywords: ['劳保'],
    })

    expect(result).not.toHaveProperty('apiKey')
    expect(result).toMatchObject({
      platforms: ['中国政府采购网'],
      keywords: ['劳保'],
    })
  })

  it('removes legacy apiKey from stored source config while restoring safe fields', () => {
    const writer = vi.fn()
    const restored = restoreSourceConfig(JSON.stringify({
      platforms: ['第三方商机服务'],
      apiKey: 'legacy-secret',
      regions: ['上海'],
    }), writer)

    expect(restored.apiKey).toBe('')
    expect(restored.platforms).toEqual(['第三方商机服务'])
    expect(writer).toHaveBeenCalledOnce()
    expect(JSON.parse(writer.mock.calls[0][0])).not.toHaveProperty('apiKey')
  })

  it('only allows http and https tender links', () => {
    expect(safeTenderUrl('https://example.com/tender')).toBe('https://example.com/tender')
    expect(safeTenderUrl('http://example.com/tender')).toBe('http://example.com/tender')
    expect(safeTenderUrl('javascript:alert(1)')).toBe('')
    expect(safeTenderUrl('/relative/path')).toBe('')
  })

  it('formats backend yuan budgets as ten-thousand-yuan display values', () => {
    expect(formatBudgetWan(15800000)).toBe('1,580')
    expect(formatBudgetWan(4500000)).toBe('450')
    expect(formatBudgetWan(12500)).toBe('1.25')
  })

  it('matches frontend permission flags to backend role boundary', () => {
    expect(buildPermissionFlags(['bidding', 'project.create'])).toMatchObject({
      canCreateTender: true,
      canManageTenders: false,
      canDeleteTenders: false,
      canSyncExternalSource: false,
    })
    expect(buildPermissionFlags(['bidding', 'settings'])).toMatchObject({
      canManageTenders: true,
      canCreateTender: true,
      canDeleteTenders: false,
      canSyncExternalSource: false,
    })
    expect(buildPermissionFlags(['all'])).toMatchObject({
      canManageTenders: true,
      canDeleteTenders: true,
      canSyncExternalSource: true,
    })
  })

  it('summarizes external sync results from the backend response shape', () => {
    expect(summarizeExternalSyncResult({
      success: true,
      message: 'Crawler executed successfully',
      savedCount: 3,
    })).toMatchObject({
      visible: true,
      saved: 3,
      skipped: 0,
      message: 'Crawler executed successfully',
    })

    expect(summarizeExternalSyncResult({
      data: { saved: 2, skipped: 1, message: 'done' },
    })).toMatchObject({
      saved: 2,
      skipped: 1,
      message: 'done',
    })
  })

  it('normalizes doc-insight tender intake fields for the manual form', () => {
    const normalized = normalizeManualTenderParseResult({
      extractedData: {
        tenderTitle: '西域仓储数字化升级采购项目',
        budget: '6800000.50',
        region: '上海',
        tenderAgency: '上海招标代理有限公司',
        deadline: '2026-05-20T18:30:00',
        bidOpeningTime: '2026-05-22T09:30:00',
        purchaserName: '上海西域采购中心',
        contactName: '王经理',
        contactPhone: '13800138000',
        customerType: 'KA 客户',
        priority: 'A',
        tenderScope: '升级仓储系统与配套设备',
        tags: '公开招标, 数字化'
      }
    })

    expect(normalized).toEqual({
      title: '西域仓储数字化升级采购项目',
      budget: 6800000.5,
      region: '上海',
      tenderAgency: '上海招标代理有限公司',
      bidOpeningTime: new Date('2026-05-22T09:30:00'),
      customerType: 'KA 客户',
      priority: 'A',
      deadline: new Date('2026-05-20T18:30:00'),
      purchaser: '上海西域采购中心',
      contact: '王经理',
      phone: '13800138000',
      description: '升级仓储系统与配套设备',
      tags: ['公开招标', '数字化'],
    })
  })

  it('keeps parsed source document metadata in manual tender create payload', () => {
    const payload = buildManualTenderPayload({
      title: '带附件标讯',
      budget: null,
      region: '上海',
      tenderAgency: '上海招标代理有限公司',
      deadline: new Date('2026-06-01T17:00:00'),
      bidOpeningTime: new Date('2026-06-03T09:30:00'),
      purchaser: '西域采购中心',
      contact: '王经理',
      phone: '13800138000',
      customerType: 'KA 客户',
      priority: 'B',
      description: '附件已由 doc-insight 解析',
      tags: ['公开招标'],
      sourceDocumentName: '招标文件.pdf',
      sourceDocumentFileType: 'application/pdf',
      sourceDocumentFileUrl: 'doc-insight://TENDER_INTAKE/manual-tender/hash-招标文件.pdf',
    })

    expect(payload).toMatchObject({
      tenderAgency: '上海招标代理有限公司',
      bidOpeningTime: '2026-06-03T09:30:00',
      customerType: 'KA 客户',
      priority: 'B',
      sourceDocumentName: '招标文件.pdf',
      sourceDocumentFileType: 'application/pdf',
      sourceDocumentFileUrl: 'doc-insight://TENDER_INTAKE/manual-tender/hash-招标文件.pdf',
    })
  })

  it('does not send removed manual industry classification', () => {
    const payload = buildManualTenderPayload({
      title: '无行业字段标讯',
      region: '上海',
      deadline: new Date('2026-06-01T17:00:00'),
    })

    expect(payload).not.toHaveProperty('industry')
  })

  it('sends date-only manual tender deadlines at end of day', () => {
    const payload = buildManualTenderPayload({
      title: '当天截止标讯',
      budget: 1000,
      region: '上海',
      industry: '政府',
      deadline: new Date('2026-05-07T00:00:00'),
    })

    expect(payload.deadline).toBe('2026-05-07T23:59:59')
  })

  it('keeps AI parsed date-only deadlines compatible with manual tender save', () => {
    const normalized = normalizeManualTenderParseResult({
      extractedData: {
        deadline: '2026-05-07',
      },
    })
    const payload = buildManualTenderPayload({
      title: '识别回填当天截止标讯',
      budget: 1000,
      region: '上海',
      industry: '政府',
      deadline: normalized.deadline,
    })

    expect(payload.deadline).toBe('2026-05-07T23:59:59')
  })

  it('converts parsed 万元 budgets to yuan before manual form backfill', () => {
    const normalized = normalizeManualTenderParseResult({
      extractedData: {
        title: '限价项目',
        budget: '最高限价 328.6 万元'
      }
    })

    expect(normalized.budget).toBe(3286000)
  })

  it('returns correct tag type for source type', () => {
    expect(getSourceTypeTagType('MANUAL')).toBe('warning')
    expect(getSourceTypeTagType('EXTERNAL')).toBe('success')
    expect(getSourceTypeTagType('UNKNOWN')).toBe('info')
  })

  it('returns correct display text for source type', () => {
    expect(getSourceTypeText('MANUAL')).toBe('人工录入')
    expect(getSourceTypeText('EXTERNAL')).toBe('外部获取')
    expect(getSourceTypeText('UNKNOWN')).toBe('UNKNOWN')
    expect(getSourceTypeText(null)).toBe('未知')
  })
})
