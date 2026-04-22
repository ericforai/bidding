// Input: bidAgent API module with mocked HTTP client
// Output: endpoint coverage for bid writing agent run lifecycle
// Pos: src/api/modules/ - API module unit tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

import httpClient from '@/api/client'
import { bidAgentApi } from './bidAgent.js'

describe('bidAgentApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('createRun(): posts to the project bid-agent run endpoint', async () => {
    httpClient.post.mockResolvedValue({ success: true, data: { runId: 'run-1', status: 'QUEUED' } })

    const result = await bidAgentApi.createRun(12, { mode: 'fullDraft' })

    expect(httpClient.post).toHaveBeenCalledWith('/api/projects/12/bid-agent/runs', { mode: 'fullDraft' })
    expect(result.data).toMatchObject({ id: 'run-1', runId: 'run-1', status: 'QUEUED' })
  })

  it('getRun(): fetches a single backend run without local fallback', async () => {
    httpClient.get.mockResolvedValue({ success: true, data: { id: 3, state: 'COMPLETED' } })

    const result = await bidAgentApi.getRun('12', '3')

    expect(httpClient.get).toHaveBeenCalledWith('/api/projects/12/bid-agent/runs/3')
    expect(result.data).toMatchObject({ id: 3, runId: 3, status: 'COMPLETED' })
  })

  it('applyRun(): writes generated content through the apply endpoint', async () => {
    const payload = { sectionIds: ['overview'] }
    httpClient.post.mockResolvedValue({ success: true, data: { documentId: 88 } })

    await bidAgentApi.applyRun(12, 'run-1', payload)

    expect(httpClient.post).toHaveBeenCalledWith('/api/projects/12/bid-agent/runs/run-1/apply', payload)
  })

  it('createReview(): posts review requests to the project review endpoint', async () => {
    const payload = { runId: 'run-1', reviewerIds: [5] }
    httpClient.post.mockResolvedValue({ success: true, data: { reviewId: 9 } })

    await bidAgentApi.createReview(12, payload)

    expect(httpClient.post).toHaveBeenCalledWith('/api/projects/12/bid-agent/reviews', payload)
  })
})
