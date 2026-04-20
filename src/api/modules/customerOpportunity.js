// Input: httpClient and customer opportunity endpoints
// Output: customerOpportunityApi and view-model hook backed by real HTTP
// Pos: src/api/modules/ - Feature API module for customer opportunity center
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { computed, ref } from 'vue'
import httpClient from '../client.js'

export const customerOpportunityApi = {
  async getCustomerInsights(params = {}) {
    const response = await httpClient.get('/api/customer-opportunities/insights', { params })
    return {
      ...response,
      data: Array.isArray(response?.data) ? response.data : [],
    }
  },

  async getPurchaseHistory(purchaserHash) {
    const response = await httpClient.get(`/api/customer-opportunities/${purchaserHash}/purchases`)
    return {
      ...response,
      data: Array.isArray(response?.data) ? response.data : [],
    }
  },

  async getPredictions(purchaserHash) {
    const response = await httpClient.get(`/api/customer-opportunities/${purchaserHash}/predictions`)
    return {
      ...response,
      data: Array.isArray(response?.data) ? response.data : [],
    }
  },

  async refreshInsights() {
    return httpClient.post('/api/customer-opportunities/refresh')
  },

  async updatePredictionStatus(id, status) {
    return httpClient.put(`/api/customer-opportunities/predictions/${id}/status`, { status })
  },

  async convertToProject(id, projectId = null) {
    return httpClient.put(`/api/customer-opportunities/predictions/${id}/convert`, { projectId })
  },
}

export function useCustomerOpportunityCenterData() {
  const customerInsights = ref([])
  const customerPurchases = ref([])
  const customerPredictions = ref([])
  const loading = ref(false)

  const salesUsers = computed(() => {
    const unique = new Map()
    customerInsights.value.forEach((item) => {
      if (!item?.salesRep) {
        return
      }
      unique.set(item.salesRep, {
        id: item.salesRep,
        name: item.salesRep,
      })
    })
    return [...unique.values()]
  })

  const loadInsights = async (params = {}) => {
    loading.value = true
    try {
      const response = await customerOpportunityApi.getCustomerInsights(params)
      customerInsights.value = response.data || []
      return response.data || []
    } finally {
      loading.value = false
    }
  }

  const loadCustomerDetail = async (customerId) => {
    const [purchasesResponse, predictionsResponse] = await Promise.all([
      customerOpportunityApi.getPurchaseHistory(customerId),
      customerOpportunityApi.getPredictions(customerId),
    ])

    customerPurchases.value = [
      ...customerPurchases.value.filter((item) => item.customerId !== customerId),
      ...(purchasesResponse.data || []),
    ]
    customerPredictions.value = [
      ...customerPredictions.value.filter((item) => item.customerId !== customerId),
      ...(predictionsResponse.data || []),
    ]

    return {
      purchases: purchasesResponse.data || [],
      predictions: predictionsResponse.data || [],
    }
  }

  return {
    loading,
    customerInsights,
    customerPurchases,
    customerPredictions,
    salesUsers,
    loadInsights,
    loadCustomerDetail,
    refreshInsights: customerOpportunityApi.refreshInsights,
    convertToProject: customerOpportunityApi.convertToProject,
    updatePredictionStatus: customerOpportunityApi.updatePredictionStatus,
  }
}

export default customerOpportunityApi
