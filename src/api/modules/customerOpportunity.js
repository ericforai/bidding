// Input: httpClient and customer opportunity endpoints
// Output: customerOpportunityApi and useCustomerOpportunityCenterData composable
// Pos: src/api/modules/ - Feature adapter module for customer opportunity center
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 客户商机中心 API
 * 真实 API 为唯一数据源
 */
import httpClient from '../client.js'
import { computed, ref } from 'vue'

export const customerOpportunityApi = {
  async getInsights() {
    return httpClient.get('/api/customer-opportunities/insights')
  },

  async getPurchases(purchaserHash) {
    return httpClient.get(`/api/customer-opportunities/${purchaserHash}/purchases`)
  },

  async getPredictions(purchaserHash) {
    return httpClient.get(`/api/customer-opportunities/${purchaserHash}/predictions`)
  },

  async refreshInsights() {
    return httpClient.post('/api/customer-opportunities/refresh')
  },

  async transitionPrediction(id, status) {
    return httpClient.put(`/api/customer-opportunities/predictions/${id}/status`, { status })
  },

  async convertPrediction(id, projectId) {
    return httpClient.put(`/api/customer-opportunities/predictions/${id}/convert`, { projectId })
  }
}

export function useCustomerOpportunityCenterData() {
  const customerInsights = ref([])
  const customerPurchases = ref([])
  const customerPredictions = ref([])
  const salesUsers = computed(() => [])

  async function loadInsights() {
    const response = await customerOpportunityApi.getInsights()
    customerInsights.value = response.data || []
  }

  async function loadPurchases(purchaserHash) {
    const response = await customerOpportunityApi.getPurchases(purchaserHash)
    customerPurchases.value = response.data || []
  }

  async function loadPredictions(purchaserHash) {
    const response = await customerOpportunityApi.getPredictions(purchaserHash)
    customerPredictions.value = response.data || []
  }

  async function refreshInsights() {
    await customerOpportunityApi.refreshInsights()
    await loadInsights()
  }

  return {
    customerInsights,
    customerPurchases,
    customerPredictions,
    salesUsers,
    loadInsights,
    loadPurchases,
    loadPredictions,
    refreshInsights
  }
}

export default customerOpportunityApi
