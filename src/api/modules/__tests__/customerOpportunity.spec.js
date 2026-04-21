// Input: useCustomerOpportunityCenterData composable factory
// Output: stable empty state contract and ref isolation coverage
// Pos: src/api/modules/__tests__/ - customer opportunity adapter unit tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { describe, expect, it } from 'vitest'

import { useCustomerOpportunityCenterData } from '../customerOpportunity.js'

describe('customerOpportunity module', () => {
  it('exposes stable empty feature state for the center page', () => {
    const state = useCustomerOpportunityCenterData()

    expect(state.mockMode).toBe(false)
    expect(state.customerInsights.value).toEqual([])
    expect(state.customerPurchases.value).toEqual([])
    expect(state.customerPredictions.value).toEqual([])
    expect(state.salesUsers.value).toEqual([])
  })

  it('creates isolated refs for each caller', () => {
    const first = useCustomerOpportunityCenterData()
    const second = useCustomerOpportunityCenterData()

    first.customerInsights.value.push({ customerId: 'one' })

    expect(first.customerInsights.value).toEqual([{ customerId: 'one' }])
    expect(second.customerInsights.value).toEqual([])
    expect(first.customerInsights).not.toBe(second.customerInsights)
    expect(first.customerPurchases).not.toBe(second.customerPurchases)
    expect(first.customerPredictions).not.toBe(second.customerPredictions)
  })
})
