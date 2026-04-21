import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const apiState = {
  mockMode: false,
  customerInsights: [],
  customerPurchases: [],
  customerPredictions: [],
  salesUsers: []
}

vi.mock('@/api/modules/customerOpportunity.js', async () => {
  const { computed, ref } = await import('vue')

  return {
    useCustomerOpportunityCenterData: () => ({
      mockMode: apiState.mockMode,
      customerInsights: ref(apiState.customerInsights),
      customerPurchases: ref(apiState.customerPurchases),
      customerPredictions: ref(apiState.customerPredictions),
      salesUsers: computed(() => apiState.salesUsers)
    })
  }
})

import { useCustomerOpportunityCenter } from './useCustomerOpportunityCenter.js'

describe('useCustomerOpportunityCenter', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    apiState.mockMode = false
    apiState.customerInsights = []
    apiState.customerPurchases = []
    apiState.customerPredictions = []
    apiState.salesUsers = []
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('starts in loading and settles into API empty state when real data source is unavailable', async () => {
    let state
    const Harness = defineComponent({
      setup() {
        state = useCustomerOpportunityCenter()
        return () => null
      }
    })

    mount(Harness)

    expect(state.loading.value).toBe(true)
    expect(state.viewState.value.showLoading).toBe(true)

    await vi.advanceTimersByTimeAsync(220)

    expect(state.loading.value).toBe(false)
    expect(state.customerOpportunityDemoEnabled).toBe(false)
    expect(state.viewState.value.showApiEmpty).toBe(true)
    expect(state.boardSummaries.value.every((item) => item.placeholder)).toBe(true)
  })
})
