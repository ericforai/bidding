// Input: auditApi with mocked HTTP client
// Output: audit log query endpoint and parameter pass-through coverage
// Pos: src/api/modules/ - API module unit tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
  },
}))

import httpClient from '@/api/client'
import { auditApi } from './audit.js'

describe('auditApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getLogs(): passes query parameters to the real audit endpoint unchanged', async () => {
    const response = {
      success: true,
      data: {
        items: [],
        summary: { totalCount: 0 },
      },
    }
    httpClient.get.mockResolvedValue(response)

    const params = {
      keyword: '创建资质',
      action: 'CREATE',
      module: 'qualification',
      operator: '李总',
      status: 'success',
      start: '2026-04-01T00:00:00',
      end: '2026-04-30T23:59:59',
    }

    const result = await auditApi.getLogs(params)

    expect(httpClient.get).toHaveBeenCalledOnce()
    expect(httpClient.get).toHaveBeenCalledWith('/api/audit', { params })
    expect(result).toBe(response)
  })
})
