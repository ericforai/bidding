import { computed, defineComponent, h, nextTick, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const { routerPush, messageInfo, messageSuccess, sourceState } = vi.hoisted(() => ({
  routerPush: vi.fn(),
  messageInfo: vi.fn(),
  messageSuccess: vi.fn(),
  sourceState: {
    loading: false,
    customerOpportunityDemoEnabled: true,
    filters: { status: '', keyword: '', sales: '', region: '', industry: '' },
    activeCustomerId: 'c-1',
    historyDrawer: false,
    isScanning: false,
    selectedCustomer: null
  }
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({
      push: routerPush
    })
  }
})

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      info: messageInfo,
      success: messageSuccess
    }
  }
})

vi.mock('@/composables/useCustomerOpportunityCenter.js', () => ({
  useCustomerOpportunityCenter: () => {
    const loading = ref(sourceState.loading)
    const filters = ref({ ...sourceState.filters })
    const activeCustomerId = ref(sourceState.activeCustomerId)
    const historyDrawer = ref(sourceState.historyDrawer)
    const isScanning = ref(sourceState.isScanning)
    const selectedCustomer = ref(sourceState.selectedCustomer)

    return {
      loading,
      customerOpportunityDemoEnabled: sourceState.customerOpportunityDemoEnabled,
      salesUsers: computed(() => []),
      filters,
      regions: computed(() => []),
      industries: computed(() => []),
      statusOptions: [],
      activeCustomerId,
      historyDrawer,
      isScanning,
      filteredCustomers: computed(() => []),
      selectedCustomer,
      customerHistory: computed(() => []),
      drawerStats: computed(() => ({ totalCount: 0, totalBudget: 0, topCategory: '未知' })),
      categoryStats: computed(() => []),
      boardSummaries: computed(() => []),
      viewState: computed(() => ({
        showLoading: loading.value,
        showDetail: !loading.value && Boolean(selectedCustomer.value),
        showOnboarding: false,
        showApiEmpty: !loading.value && !sourceState.customerOpportunityDemoEnabled && !selectedCustomer.value
      })),
      selectCustomer: vi.fn(),
      selectFirstHighValue: vi.fn()
    }
  }
}))

vi.mock('@/views/Bidding/customer-opportunity/CustomerOpportunityBoard.vue', () => ({
  default: defineComponent({
    name: 'CustomerOpportunityBoard',
    setup() {
      return () => h('div', { 'data-test': 'board' })
    }
  })
}))

vi.mock('@/views/Bidding/customer-opportunity/CustomerOpportunityFiltersTable.vue', () => ({
  default: defineComponent({
    name: 'CustomerOpportunityFiltersTable',
    emits: ['update:filters', 'select-customer'],
    setup() {
      return () => h('div', { 'data-test': 'filters-table' })
    }
  })
}))

vi.mock('@/views/Bidding/customer-opportunity/CustomerOpportunityDetail.vue', () => ({
  default: defineComponent({
    name: 'CustomerOpportunityDetail',
    emits: ['open-history', 'select-first-high-value', 'recommend-project', 'go-bidding'],
    setup() {
      return () => h('div', { 'data-test': 'detail-panel' })
    }
  })
}))

vi.mock('@/views/Bidding/customer-opportunity/CustomerOpportunityHistoryDrawer.vue', () => ({
  default: defineComponent({
    name: 'CustomerOpportunityHistoryDrawer',
    props: ['modelValue'],
    emits: ['update:modelValue'],
    setup() {
      return () => h('div', { 'data-test': 'history-drawer' })
    }
  })
}))

import CustomerOpportunityCenter from './CustomerOpportunityCenter.vue'

function mountPage() {
  return mount(CustomerOpportunityCenter, {
    global: {
      stubs: {
        transition: false,
        'el-button': defineComponent({
          name: 'ElButton',
          props: ['disabled', 'loading', 'type'],
          emits: ['click'],
          inheritAttrs: false,
          setup(props, { emit, slots, attrs }) {
            return () =>
              h(
                'button',
                {
                  ...attrs,
                  disabled: props.disabled,
                  'data-loading': props.loading,
                  onClick: () => emit('click')
                },
                slots.default ? slots.default() : []
              )
          }
        }),
        'el-icon': defineComponent({
          name: 'ElIcon',
          setup(_, { slots }) {
            return () => h('span', slots.default ? slots.default() : [])
          }
        })
      }
    }
  })
}

describe('CustomerOpportunityCenter', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sourceState.loading = false
    sourceState.customerOpportunityDemoEnabled = true
    sourceState.isScanning = false
    sourceState.historyDrawer = false
    sourceState.activeCustomerId = 'c-1'
    sourceState.filters = { status: '', keyword: '', sales: '', region: '', industry: '' }
    sourceState.selectedCustomer = {
      customerId: 'c-1',
      customerName: '华北医疗集团',
      industry: '医疗',
      region: '华北',
      mainCategories: ['耗材', '设备'],
      prediction: {
        opportunityId: 'opp-1',
        suggestedProjectName: '华北医疗年度集采',
        predictedCategory: '耗材',
        predictedBudgetMin: 200,
        predictedBudgetMax: 400,
        predictedWindow: '2026-05',
        confidence: 0.82,
        reasoningSummary: '预算窗口接近',
        convertedProjectId: ''
      }
    }
  })

  it('pushes project create route with stable customer opportunity query payload', async () => {
    const wrapper = mountPage()

    await wrapper.find('.btn-primary').trigger('click')

    expect(routerPush).toHaveBeenCalledWith({
      path: '/project/create',
      query: {
        projectName: '华北医疗年度集采',
        customerName: '华北医疗集团',
        industry: '医疗',
        region: '华北',
        budget: '300',
        deadline: '2026-05-28',
        tags: '耗材,设备',
        description: '基于历史采购规律预测，建议围绕“耗材”提前立项跟进。',
        remark: '预测时间窗口：2026-05；置信度：82%',
        sourceModule: 'customer-opportunity-center',
        sourceCustomerId: 'c-1',
        sourceCustomerName: '华北医疗集团',
        sourceOpportunityId: 'opp-1',
        sourceReasoningSummary: '预算窗口接近'
      }
    })
  })

  it('pushes converted project detail route when project already exists', async () => {
    sourceState.selectedCustomer.prediction.convertedProjectId = 88
    const wrapper = mountPage()

    await wrapper.find('.btn-primary').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/project/88')
  })

  it('renders refresh action as disabled when demo entry is hidden in real mode', async () => {
    sourceState.customerOpportunityDemoEnabled = false
    const wrapper = mountPage()
    const refreshButton = wrapper.find('.btn-refresh')

    expect(refreshButton.attributes('disabled')).toBeDefined()
    expect(refreshButton.text()).toContain('洞察未接入')
    await refreshButton.trigger('click')
    expect(messageSuccess).not.toHaveBeenCalled()
    expect(messageInfo).not.toHaveBeenCalled()
  })
})
