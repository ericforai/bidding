// Input: bid result normalizer fixtures, bidResultsApi, and mocked HTTP client
// Output: vitest coverage for bid result normalization and command endpoint wiring
// Pos: src/api/modules/ - Bid result API tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

import httpClient from '@/api/client'
import { bidResultsApi } from './bidResults.js'
import {
  normalizeCompetitorReport,
  normalizeDetail,
  normalizeOverview,
  normalizeReminder
} from './bidResults.normalizers.js'

describe('bidResults normalizers', () => {
  it('maps overview fields from backend names', () => {
    expect(normalizeOverview({
      pendingFetchCount: 3,
      pendingReminderCount: 5,
      competitorCount: 7
    })).toEqual({
      lastSyncTime: '',
      pendingCount: 3,
      uploadPending: 5,
      competitorCount: 7
    })
  })

  it('maps reminder owner and type fields', () => {
    expect(normalizeReminder({
      id: 1,
      ownerName: '张三',
      reminderType: 'NOTICE',
      status: 'UPLOADED'
    })).toMatchObject({
      id: 1,
      owner: '张三',
      type: 'notice',
      status: 'uploaded'
    })
  })

  it('unwraps detail payload and keeps attachments and competitors', () => {
    const detail = normalizeDetail({
      fetchResult: {
        id: 9,
        projectId: 18,
        projectName: '西域项目',
        result: 'WON',
        amount: 1200,
        contractStartDate: '2026-01-01',
        contractEndDate: '2026-12-31',
        contractDurationMonths: 12,
        remark: '备注',
        skuCount: 66
      },
      reminder: { id: 1, reminderType: 'NOTICE' },
      noticeAttachment: { documentId: 11, name: 'notice.pdf' },
      competitorWins: [{ competitorName: '竞品A' }]
    })

    expect(detail).toMatchObject({
      id: 9,
      projectId: 18,
      projectName: '西域项目',
      result: 'won',
      reminders: [{ id: 1, type: 'notice' }],
      attachments: {
        noticeDocument: { id: 11, name: 'notice.pdf' }
      },
      competitors: [{ company: '竞品A' }]
    })
  })

  it('maps competitor payment terms field', () => {
    expect(normalizeCompetitorReport({
      company: '竞品B',
      paymentTerms: '月结30天',
      projectCount: 2
    })).toMatchObject({
      company: '竞品B',
      payment: '月结30天',
      projectCount: 2
    })
  })
})

describe('bidResultsApi command endpoints', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('confirmWithData(): calls the backend confirm-with-data endpoint', async () => {
    httpClient.post.mockResolvedValue({
      success: true,
      data: { id: 8, result: 'WON', status: 'CONFIRMED' },
    })

    const result = await bidResultsApi.confirmWithData(8, { result: 'won', amount: 1200 })

    expect(httpClient.post).toHaveBeenCalledWith('/api/bid-results/fetch-results/8/confirm-with-data', {
      result: 'won',
      amount: 1200,
    })
    expect(result.data).toMatchObject({ id: 8, result: 'won', status: 'confirmed' })
  })

  it('uploadProjectDocument(): stores attachment metadata through the project document endpoint', async () => {
    httpClient.post.mockResolvedValue({ success: true, data: { id: 18 } })

    await bidResultsApi.uploadProjectDocument(5, {
      file: { name: 'notice.pdf', size: 1024, type: 'application/pdf' },
      documentCategory: 'BID_RESULT_NOTICE',
      linkedEntityType: 'BID_RESULT',
      linkedEntityId: 8,
    })

    expect(httpClient.post).toHaveBeenCalledWith('/api/projects/5/documents', {
      name: 'notice.pdf',
      size: '1024',
      fileType: 'application/pdf',
      documentCategory: 'BID_RESULT_NOTICE',
      linkedEntityType: 'BID_RESULT',
      linkedEntityId: 8,
      fileUrl: '',
      uploaderId: null,
      uploaderName: '',
    })
  })

  it('createCompetitorWin(): maps frontend competitor form fields to backend DTO fields', async () => {
    httpClient.post.mockResolvedValue({ success: true, data: { id: 1, competitorName: '竞品A' } })

    await bidResultsApi.createCompetitorWin({
      projectId: 5,
      company: '竞品A',
      paymentTerms: '月结30天',
    })

    expect(httpClient.post).toHaveBeenCalledWith('/api/bid-results/competitor-wins', {
      competitorId: null,
      competitorName: '竞品A',
      projectId: 5,
      skuCount: null,
      category: '',
      discount: '',
      paymentTerms: '月结30天',
      wonAt: null,
      amount: null,
      notes: '',
    })
  })
})
