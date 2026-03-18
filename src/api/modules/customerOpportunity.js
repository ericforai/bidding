import { computed, ref } from 'vue'
import { isMockMode } from '../config.js'
import { mockData } from '../mock.js'
import { loadDemoState } from '@/utils/demoPersistence'

const DEMO_SALES_REPS = ['小王', '张经理', '李工']

function pickSalesRep(salesRep) {
  if (salesRep) return salesRep
  return DEMO_SALES_REPS[Math.floor(Math.random() * DEMO_SALES_REPS.length)]
}

function normalizeInsight(item) {
  const base = mockData.customerInsights.find((entry) => entry.customerId === item.customerId) || {}
  return {
    ...base,
    ...item,
    salesRep: pickSalesRep(item.salesRep || base.salesRep),
  }
}

function normalizePrediction(item) {
  const base = mockData.customerPredictions.find((entry) => entry.customerId === item.customerId) || {}
  return {
    ...base,
    ...item,
  }
}

function loadMockInsights() {
  const stored = loadDemoState('customer-insights', [])
  if (stored.length) {
    return stored.map(normalizeInsight)
  }

  return (mockData.customerInsights || []).map((item) => ({
    ...item,
    salesRep: pickSalesRep(item.salesRep),
  }))
}

function loadMockPredictions() {
  const stored = loadDemoState('customer-predictions', [])
  if (stored.length) {
    return stored.map(normalizePrediction)
  }

  return [...(mockData.customerPredictions || [])]
}

export function useCustomerOpportunityCenterData() {
  const mockMode = isMockMode()

  const customerInsights = ref(mockMode ? loadMockInsights() : [])
  const customerPurchases = ref(mockMode ? [...(mockData.customerPurchases || [])] : [])
  const customerPredictions = ref(mockMode ? loadMockPredictions() : [])

  const salesUsers = computed(() => {
    if (!mockMode) return []
    return (mockData.users || []).filter((user) => user.role !== 'admin')
  })

  return {
    isMockMode: mockMode,
    customerInsights,
    customerPurchases,
    customerPredictions,
    salesUsers,
  }
}
