import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const routerPush = vi.fn()
const routeState = {
  params: { id: 'T9001' },
}

const getDetail = vi.fn()
const getLatestScore = vi.fn()
const generateScore = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
  useRoute: () => routeState,
}))

vi.mock('@/api', () => ({
  tendersApi: { getDetail },
  bidMatchScoringApi: { getLatestScore, generateScore },
}))

const elMessage = {
  success: vi.fn(),
  warning: vi.fn(),
  error: vi.fn(),
}

vi.mock('element-plus', () => ({
  ElMessage: elMessage,
}))

const { useBiddingDetailPage } = await import('./useBiddingDetailPage.js')

function createHarness() {
  return defineComponent({
    template: '<div />',
    setup() {
      return useBiddingDetailPage()
    },
  })
}

describe('useBiddingDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeState.params = { id: 'T9001' }
    getDetail.mockResolvedValue({
      success: true,
      data: {
        id: 'T9001',
        industry: '政府',
        originalUrl: 'https://example.com/tender',
      },
    })
    getLatestScore.mockResolvedValue({
      success: true,
      data: {
        id: 'S9001',
        tenderId: 'T9001',
        totalScore: 86,
        modelVersion: '2026.04',
        status: 'READY',
        dimensions: [
          { key: 'budgetFit', name: '预算匹配', score: 90, weight: 60 },
          { key: 'delivery', name: '交付窗口', score: 80, weight: 40 },
        ],
      },
    })
    generateScore.mockResolvedValue({
      success: true,
      data: {
        id: 'S9002',
        tenderId: 'T9001',
        totalScore: 91,
        modelVersion: '2026.05',
        status: 'READY',
        dimensions: [{ key: 'profit', name: '利润空间', score: 91, weight: 100 }],
      },
    })
  })

  it('loads tender detail and latest match score', async () => {
    const wrapper = mount(createHarness())
    await flushPromises()

    expect(getDetail).toHaveBeenCalledWith('T9001')
    expect(getLatestScore).toHaveBeenCalledWith('T9001')
    expect(wrapper.vm.tender.id).toBe('T9001')
    expect(wrapper.vm.matchScore.totalScore).toBe(86)
    expect(wrapper.vm.matchScoreState).toBe('ready')
    expect(wrapper.vm.probabilityRate).toBe(4)
    expect(wrapper.vm.matchScore.dimensions.map((dimension) => dimension.name)).toEqual(['预算匹配', '交付窗口'])
  })

  it('reports error when detail request fails', async () => {
    getDetail.mockResolvedValue({ success: false, message: '详情加载失败' })
    mount(createHarness())
    await flushPromises()

    expect(elMessage.error).toHaveBeenCalledWith('详情加载失败')
  })

  it('shows empty action when latest score is absent', async () => {
    getLatestScore.mockResolvedValue({ success: true, data: null })
    const wrapper = mount(createHarness())
    await flushPromises()

    expect(wrapper.vm.matchScoreState).toBe('empty')
    expect(wrapper.vm.scoreEmptyText).toBe('生成匹配评分')
  })

  it('keeps not-configured and failed scoring states visible', async () => {
    getLatestScore.mockResolvedValueOnce({ success: true, data: { status: 'NOT_CONFIGURED' } })
    const notConfiguredWrapper = mount(createHarness())
    await flushPromises()
    expect(notConfiguredWrapper.vm.matchScoreState).toBe('not-configured')
    expect(notConfiguredWrapper.vm.scoreEmptyDescription).toBe('请先在系统设置中启用投标匹配评分模型。')

    getLatestScore.mockResolvedValueOnce({ success: true, data: { status: 'FAILED', failureReason: '生成失败' } })
    const failedWrapper = mount(createHarness())
    await flushPromises()
    expect(failedWrapper.vm.matchScoreState).toBe('failed')
    expect(failedWrapper.vm.scoreEmptyDescription).toBe('生成失败')
  })

  it('generates score and refreshes the latest result', async () => {
    getLatestScore
      .mockResolvedValueOnce({ success: true, data: null })
      .mockResolvedValueOnce({
        success: true,
        data: { id: 'S9002', totalScore: 91, status: 'READY', dimensions: [] },
      })
    const wrapper = mount(createHarness())
    await flushPromises()

    await wrapper.vm.handleGenerateMatchScore()

    expect(generateScore).toHaveBeenCalledWith('T9001')
    expect(wrapper.vm.matchScore.totalScore).toBe(91)
    expect(elMessage.success).toHaveBeenCalledWith('匹配评分已生成')
  })

  it('opens only safe tender urls', async () => {
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
    const wrapper = mount(createHarness())
    await flushPromises()

    wrapper.vm.handleViewOriginal()
    expect(openSpy).toHaveBeenCalledWith('https://example.com/tender', '_blank', 'noopener,noreferrer')

    wrapper.vm.tender = { ...wrapper.vm.tender, originalUrl: 'javascript:alert(1)' }
    wrapper.vm.handleViewOriginal()
    expect(elMessage.warning).toHaveBeenCalledWith('该标讯暂无官网公告链接')
  })

  it('navigates to project create with selected tender', async () => {
    const wrapper = mount(createHarness())
    await flushPromises()

    wrapper.vm.handleParticipate()
    expect(routerPush).toHaveBeenCalledWith({
      path: '/project/create',
      query: { tenderId: 'T9001' },
    })
  })
})
