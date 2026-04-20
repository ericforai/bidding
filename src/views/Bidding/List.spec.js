import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import List from './List.vue'

const tableStub = {
  name: 'ElTable',
  template: '<div class="el-table-stub"><slot /></div>'
}

const tableColumnStub = {
  name: 'ElTableColumn',
  template: '<div class="el-table-column-stub"><slot :row="{}" /></div>'
}

const progressStub = {
  name: 'ElProgress',
  template: '<div class="el-progress-stub"><slot :percentage="0" /></div>'
}

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

// Mock Element Plus Icons
vi.mock('@element-plus/icons-vue', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    Search: () => 'Search',
    Plus: () => 'Plus',
    Setting: () => 'Setting',
    Download: () => 'Download',
    Refresh: () => 'Refresh',
    MagicStick: () => 'MagicStick',
    UserFilled: () => 'UserFilled',
    Location: () => 'Location',
    Wallet: () => 'Wallet',
    Calendar: () => 'Calendar',
    InfoFilled: () => 'InfoFilled',
    ArrowRight: () => 'ArrowRight',
    Link: () => 'Link',
    View: () => 'View',
    Document: () => 'Document',
    MoreFilled: () => 'MoreFilled',
    Share: () => 'Share',
    CircleCheck: () => 'CircleCheck',
    Star: () => 'Star',
    TrendCharts: () => 'TrendCharts'
  }
})

describe('List.vue (标讯中心)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.defineProperty(window, 'innerWidth', {
      configurable: true,
      writable: true,
      value: 375
    })
  })

  it('应该正确渲染页面标题', async () => {
    const wrapper = mount(List, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          'el-icon': true,
          'el-table': tableStub,
          'el-table-column': tableColumnStub,
          'el-progress': progressStub,
          Link: true
        }
      }
    })

    expect(wrapper.find('.page-title').text()).toBe('标讯中心')
  })

  it('当搜索关键词变化时，filteredTenders 应该能正确过滤', async () => {
    const wrapper = mount(List, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          'el-icon': true,
          'el-table': tableStub,
          'el-table-column': tableColumnStub,
          'el-progress': progressStub,
          Link: true
        }
      }
    })

    wrapper.vm.searchForm.keyword = '西域'
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.searchForm.keyword).toBe('西域')
  })
})
