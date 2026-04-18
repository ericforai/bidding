import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import List from './List.vue'

// Mock API 模块
vi.mock('@/api', () => ({
  tendersApi: {
    getList: vi.fn(() => Promise.resolve({ success: true, data: [] })),
    fetchExternalTenders: vi.fn(() => Promise.resolve({ success: true })),
    fetchFromCeb: vi.fn(() => Promise.resolve({ success: true }))
  },
  authApi: {
    getCurrentUser: vi.fn(() => ({ role: 'ADMIN' }))
  },
  API_CONFIG: { mode: 'api' },
  isCommercialMode: vi.fn(() => false),
  getApiUrl: vi.fn((path) => path)
}))

// Mock Vue Router
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  }),
  useRoute: () => ({
    query: {}
  }),
  createRouter: vi.fn(() => ({
    beforeEach: vi.fn(),
    afterEach: vi.fn(),
    push: vi.fn(),
    install: vi.fn()
  })),
  createWebHistory: vi.fn()
}))

// 不 mock @element-plus/icons-vue：List.vue 的图标集合会随业务变化，
// 保留真实导出避免测试清单漂移。

describe('List.vue (标讯中心)', () => {
  // shallowMount 自动 stub 全部子组件，避开 el-table 等重组件的作用域插槽空参调用问题。
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('应该正确渲染页面标题', async () => {
    const wrapper = shallowMount(List)

    expect(wrapper.find('.page-title').text()).toBe('标讯中心')
  })

  it('当搜索关键词变化时，filteredTenders 应该能正确过滤', async () => {
    const wrapper = shallowMount(List)

    wrapper.vm.searchForm.keyword = '西域'
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.searchForm.keyword).toBe('西域')
  })
})
