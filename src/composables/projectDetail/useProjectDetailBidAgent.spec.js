// Input: useProjectDetailBidAgent with mocked bid-agent API and router
// Output: run lifecycle state, apply/review calls, and editor navigation coverage
// Pos: src/composables/projectDetail/ - Project Detail composable tests

import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const apiMocks = vi.hoisted(() => ({
  createRun: vi.fn(),
  getRun: vi.fn(),
  applyRun: vi.fn(),
  createReview: vi.fn(),
}))

vi.mock('@/api/modules/bidAgent.js', () => ({
  bidAgentApi: apiMocks,
}))

import { useProjectDetailBidAgent } from './useProjectDetailBidAgent.js'

function createContext() {
  return {
    route: { params: { id: '12' } },
    router: { push: vi.fn() },
    project: ref({ id: 12, name: '测试项目' }),
    message: {
      success: vi.fn(),
      warning: vi.fn(),
      error: vi.fn(),
    },
  }
}

describe('useProjectDetailBidAgent', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('creates a run and keeps the drawer open for status tracking', async () => {
    apiMocks.createRun.mockResolvedValue({ success: true, data: { runId: 'run-7', status: 'QUEUED' } })
    const context = createContext()
    const agent = useProjectDetailBidAgent(context)

    const run = await agent.createRun({ mode: 'draft' })

    expect(apiMocks.createRun).toHaveBeenCalledWith(12, { mode: 'draft' })
    expect(agent.drawerVisible.value).toBe(true)
    expect(agent.currentRunId.value).toBe('run-7')
    expect(run.status).toBe('QUEUED')
    expect(context.message.success).toHaveBeenCalledWith('AI 初稿生成任务已启动')
  })

  it('fetches the latest run state using the current run id', async () => {
    apiMocks.createRun.mockResolvedValue({ success: true, data: { runId: 'run-8', status: 'QUEUED' } })
    apiMocks.getRun.mockResolvedValue({ success: true, data: { runId: 'run-8', status: 'COMPLETED' } })
    const agent = useProjectDetailBidAgent(createContext())

    await agent.createRun()
    await agent.fetchRun()

    expect(apiMocks.getRun).toHaveBeenCalledWith(12, 'run-8')
    expect(agent.currentRun.value.status).toBe('COMPLETED')
  })

  it('applies a run and can navigate to the document editor with metadata', async () => {
    const context = createContext()
    const agent = useProjectDetailBidAgent(context)
    agent.currentRun.value = { runId: 'run-9', status: 'COMPLETED' }
    apiMocks.applyRun.mockResolvedValue({
      success: true,
      data: { projectId: 12, documentId: 55, jobId: 'job-3' },
    })

    await agent.applyBidAgentResult({ selectedSectionIds: ['overview'] })
    agent.goToEditor()

    expect(apiMocks.applyRun).toHaveBeenCalledWith(12, 'run-9', { selectedSectionIds: ['overview'] })
    expect(context.router.push).toHaveBeenCalledWith({
      name: 'DocumentEditor',
      params: { id: '12' },
      query: {
        bidAgentRunId: 'run-9',
        documentId: '55',
        jobId: 'job-3',
      },
    })
  })

  it('starts a review for the current run', async () => {
    const agent = useProjectDetailBidAgent(createContext())
    agent.currentRun.value = { id: 21, status: 'COMPLETED' }
    apiMocks.createReview.mockResolvedValue({ success: true, data: { reviewId: 6 } })

    const result = await agent.createReview({ reviewerIds: [3] })

    expect(apiMocks.createReview).toHaveBeenCalledWith(12, { runId: 21, reviewerIds: [3] })
    expect(result.reviewId).toBe(6)
  })
})
