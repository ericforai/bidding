import { computed, onMounted, ref } from 'vue'
import { useCustomerOpportunityCenterData } from '@/api/modules/customerOpportunity.js'
import {
  CUSTOMER_OPPORTUNITY_STATUS_OPTIONS,
  buildBoardSummaries,
  buildCategoryStats,
  buildCustomerHistory,
  buildDrawerStats,
  buildSelectedCustomer,
  filterCustomers,
  resolveCustomerOpportunityViewState
} from '@/views/Bidding/customerOpportunityView.js'

export function useCustomerOpportunityCenter() {
  const { mockMode, customerInsights, customerPurchases, customerPredictions, salesUsers } = useCustomerOpportunityCenterData()

  const customerOpportunityDemoEnabled = Boolean(mockMode)
  const loading = ref(true)
  const filters = ref({ status: '', keyword: '', sales: '', region: '', industry: '' })
  const activeCustomerId = ref('')
  const historyDrawer = ref(false)
  const isScanning = ref(false)

  const regions = computed(() => [...new Set(customerInsights.value.map((item) => item.region))].filter(Boolean))
  const industries = computed(() => [...new Set(customerInsights.value.map((item) => item.industry))].filter(Boolean))
  const statusOptions = CUSTOMER_OPPORTUNITY_STATUS_OPTIONS

  const filteredCustomers = computed(() => filterCustomers(customerInsights.value, filters.value))
  const selectedCustomer = computed(() =>
    buildSelectedCustomer(customerInsights.value, customerPurchases.value, customerPredictions.value, activeCustomerId.value)
  )
  const customerHistory = computed(() => buildCustomerHistory(selectedCustomer.value, customerPurchases.value))
  const drawerStats = computed(() => buildDrawerStats(customerHistory.value))
  const categoryStats = computed(() => buildCategoryStats(customerHistory.value))
  const boardSummaries = computed(() =>
    buildBoardSummaries({
      demoEnabled: customerOpportunityDemoEnabled,
      customerInsights: customerInsights.value,
      customerPredictions: customerPredictions.value
    })
  )
  const viewState = computed(() =>
    resolveCustomerOpportunityViewState({
      loading: loading.value,
      demoEnabled: customerOpportunityDemoEnabled,
      selectedCustomer: selectedCustomer.value
    })
  )

  onMounted(() => {
    const delay = customerOpportunityDemoEnabled ? 800 : 220
    setTimeout(() => {
      loading.value = false
    }, delay)
  })

  const selectCustomer = (row) => {
    activeCustomerId.value = row?.customerId || ''
  }

  const selectFirstHighValue = () => {
    if (!customerOpportunityDemoEnabled) {
      return
    }

    const first = customerInsights.value.find((item) => item.opportunityScore >= 85)
    if (first) {
      activeCustomerId.value = first.customerId
    }
  }

  return {
    loading,
    customerOpportunityDemoEnabled,
    customerInsights,
    customerPurchases,
    customerPredictions,
    salesUsers,
    filters,
    regions,
    industries,
    statusOptions,
    activeCustomerId,
    historyDrawer,
    isScanning,
    filteredCustomers,
    selectedCustomer,
    customerHistory,
    drawerStats,
    categoryStats,
    boardSummaries,
    viewState,
    selectCustomer,
    selectFirstHighValue
  }
}
