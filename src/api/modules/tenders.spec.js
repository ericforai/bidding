// Input: tenders API module with mocked HTTP client
// Output: tender search parameter passthrough and backend-sourced list coverage
// Pos: src/api/modules/ - API module unit tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

import httpClient from '@/api/client'
import { tendersApi } from './tenders.js'

describe('tendersApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getList(): sends search params to the backend without local re-filtering', async () => {
    httpClient.get.mockResolvedValue({
      success: true,
      data: [
        { id: 1, title: '华东数据中心 GPU 算力平台采购项目', region: '上海' },
        { id: 2, title: '华北办公电脑采购项目', region: '北京' }
      ]
    })

    const params = {
      keyword: 'GPU',
      status: 'PENDING',
      source: '中国招标投标公共服务平台',
      region: '上海',
      industry: '数据中心',
      purchaserName: '西域采购',
      purchaserHash: 'hash-shanghai-xiyu',
      budgetMin: 4000000,
      budgetMax: 6000000,
      deadlineFrom: '2026-05-01T00:00:00',
      deadlineTo: '2026-05-10T23:59:59',
      publishDateFrom: '2026-04-01',
      publishDateTo: '2026-04-30',
      aiScoreMin: 90,
      aiScoreMax: 95
    }

    const result = await tendersApi.getList(params)

    expect(httpClient.get).toHaveBeenCalledWith('/api/tenders', { params })
    expect(result.success).toBe(true)
    expect(result.data).toHaveLength(2)
    expect(result.total).toBe(2)
  })

  it('create(): posts manual tender payload to the real backend endpoint', async () => {
    const payload = {
      title: '人工录入标讯',
      budget: 1200000,
      region: '上海',
      industry: '数据中心',
      purchaserName: '上海西域采购中心',
      purchaserHash: 'hash-shanghai-xiyu',
      publishDate: '2026-04-21',
      deadline: '2026-05-08T18:00:00',
      contactName: '王经理',
      contactPhone: '13800138000',
      description: '人工录入测试',
      tags: ['数据中心'],
      source: 'manual',
      status: 'PENDING'
    }
    httpClient.post.mockResolvedValue({ success: true, data: { id: 10, ...payload } })

    await tendersApi.create(payload)

    expect(httpClient.post).toHaveBeenCalledWith('/api/tenders', payload)
  })
})
