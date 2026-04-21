import { describe, expect, it } from 'vitest'
import {
  EMPTY_CUSTOMER_PREDICTION,
  buildCategoryStats,
  buildCreateProjectQuery,
  buildCustomerHistory,
  buildDrawerStats,
  buildSelectedCustomer,
  filterCustomers,
  resolveCustomerOpportunityViewState
} from './customerOpportunityView.js'

const customers = [
  {
    customerId: 'c-1',
    customerName: '华北医疗集团',
    region: '华北',
    industry: '医疗',
    salesRep: '张三',
    status: 'recommend',
    mainCategories: ['耗材', '设备']
  },
  {
    customerId: 'c-2',
    customerName: '华东制造中心',
    region: '华东',
    industry: '制造',
    salesRep: '李四',
    status: 'watch',
    mainCategories: ['机加']
  }
]

const purchases = [
  { recordId: 'r-1', customerId: 'c-1', title: 'A', category: '耗材', budget: 120, publishDate: '2026-03-10' },
  { recordId: 'r-2', customerId: 'c-1', title: 'B', category: '设备', budget: 300, publishDate: '2026-04-10' },
  { recordId: 'r-3', customerId: 'c-1', title: 'C', category: '耗材', budget: 80, publishDate: '2026-01-01' },
  { recordId: 'r-4', customerId: 'c-2', title: 'D', category: '机加', budget: 50, publishDate: '2026-02-02' }
]

describe('customerOpportunityView', () => {
  it('filters customers by combined criteria', () => {
    const result = filterCustomers(customers, {
      keyword: '华北',
      region: '华北',
      industry: '医疗',
      sales: '张三',
      status: 'recommend'
    })

    expect(result).toHaveLength(1)
    expect(result[0].customerId).toBe('c-1')
  })

  it('builds selected customer with fallback prediction when prediction is missing', () => {
    const selectedCustomer = buildSelectedCustomer(customers, purchases, [], 'c-1')

    expect(selectedCustomer.customerId).toBe('c-1')
    expect(selectedCustomer.purchaseHistory).toHaveLength(3)
    expect(selectedCustomer.prediction).toEqual(EMPTY_CUSTOMER_PREDICTION)
  })

  it('aggregates drawer stats and category distribution from sorted customer history', () => {
    const selectedCustomer = buildSelectedCustomer(customers, purchases, [], 'c-1')
    const history = buildCustomerHistory(selectedCustomer, purchases)
    const drawerStats = buildDrawerStats(history)
    const categoryStats = buildCategoryStats(history)

    expect(history.map((item) => item.recordId)).toEqual(['r-2', 'r-1', 'r-3'])
    expect(drawerStats).toEqual({
      totalCount: 3,
      totalBudget: 500,
      topCategory: '耗材'
    })
    expect(categoryStats).toEqual([
      { name: '耗材', count: 2, percent: 67, color: '#3b82f6' },
      { name: '设备', count: 1, percent: 33, color: '#8b5cf6' }
    ])
  })

  it('builds createProject query with stable customer-opportunity keys', () => {
    const selectedCustomer = buildSelectedCustomer(
      customers,
      purchases,
      [
        {
          customerId: 'c-1',
          opportunityId: 'opp-1',
          suggestedProjectName: '华北医疗年度集采',
          predictedCategory: '耗材',
          predictedBudgetMin: 200,
          predictedBudgetMax: 400,
          predictedWindow: '2026-05',
          confidence: 0.82,
          reasoningSummary: '预算窗口接近'
        }
      ],
      'c-1'
    )

    expect(buildCreateProjectQuery(selectedCustomer)).toEqual({
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
    })
  })

  it('resolves loading and api empty states without enabling hidden demo entry', () => {
    expect(resolveCustomerOpportunityViewState({
      loading: true,
      demoEnabled: false,
      selectedCustomer: null
    })).toEqual({
      showLoading: true,
      showDetail: false,
      showOnboarding: false,
      showApiEmpty: false
    })

    expect(resolveCustomerOpportunityViewState({
      loading: false,
      demoEnabled: false,
      selectedCustomer: null
    })).toEqual({
      showLoading: false,
      showDetail: false,
      showOnboarding: false,
      showApiEmpty: true
    })
  })
})
