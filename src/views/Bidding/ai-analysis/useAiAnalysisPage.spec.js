import { defineComponent, nextTick } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const routerPush = vi.fn()
const routerBack = vi.fn()
const routeState = {
  params: { id: 'T001', fromList: true },
}

const getDetail = vi.fn()
const getAnalysis = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush, back: routerBack }),
  useRoute: () => routeState,
}))

vi.mock('@/api', () => ({
  tendersApi: { getDetail },
  aiApi: { bid: { getAnalysis } },
}))

const elMessage = {
  info: vi.fn(),
  success: vi.fn(),
  warning: vi.fn(),
  error: vi.fn(),
}

vi.mock('element-plus', () => ({
  ElMessage: elMessage,
  ElMessageBox: {
    confirm: vi.fn(() => Promise.resolve()),
  },
}))

const { useAiAnalysisPage } = await import('./useAiAnalysisPage.js')

function createHarness() {
  return defineComponent({
    template: '<div />',
    setup() {
      return useAiAnalysisPage()
    },
  })
}

describe('useAiAnalysisPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeState.params = { id: 'T001', fromList: true }
    getDetail.mockResolvedValue({ success: true, data: { id: 'T001', title: '测试标讯' } })
    getAnalysis.mockResolvedValue({
      success: true,
      data: {
        winScore: 88,
        suggestion: '建议跟进',
        dimensionScores: [{ name: '客户关系', score: 80 }],
        risks: [],
        autoTasks: [],
      },
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('loads tender info and ai analysis on mounted', async () => {
    const wrapper = mount(createHarness())
    await flushPromises()

    expect(getDetail).toHaveBeenCalledWith('T001')
    expect(getAnalysis).toHaveBeenCalledWith('T001')
    expect(wrapper.vm.tenderInfo.title).toBe('测试标讯')
    expect(wrapper.vm.analysisData.winScore).toBe(88)
  })

  it('reports backend error when analysis request fails', async () => {
    getAnalysis.mockResolvedValue({ success: false, message: 'AI服务异常' })
    const wrapper = mount(createHarness())
    await flushPromises()

    expect(wrapper.vm.analysisData).toBe(null)
    expect(elMessage.error).toHaveBeenCalledWith('AI服务异常')
  })

  it('clears parsing timer on unmount when parsing animation is enabled', async () => {
    vi.useFakeTimers()
    routeState.params = { id: 'T001' }
    const clearIntervalSpy = vi.spyOn(globalThis, 'clearInterval')

    const wrapper = mount(createHarness())
    await nextTick()
    expect(wrapper.vm.showParsingDialog).toBe(true)

    wrapper.unmount()
    expect(clearIntervalSpy).toHaveBeenCalled()
  })
})
