import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const routerPush = vi.fn()
const routeState = {
  params: { id: 'T9001' },
}

const getDetail = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
  useRoute: () => routeState,
}))

vi.mock('@/api', () => ({
  tendersApi: { getDetail },
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
        aiScore: 92,
        industry: '政府',
        originalUrl: 'https://example.com/tender',
      },
    })
  })

  it('loads tender detail and derives probability rate', async () => {
    const wrapper = mount(createHarness())
    await flushPromises()

    expect(getDetail).toHaveBeenCalledWith('T9001')
    expect(wrapper.vm.tender.id).toBe('T9001')
    expect(wrapper.vm.probabilityRate).toBe(5)
  })

  it('reports error when detail request fails', async () => {
    getDetail.mockResolvedValue({ success: false, message: '详情加载失败' })
    mount(createHarness())
    await flushPromises()

    expect(elMessage.error).toHaveBeenCalledWith('详情加载失败')
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
