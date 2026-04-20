// Input: Vue refs/computed only
// Output: customer opportunity view-model adapters and feature-state helpers
// Pos: src/api/modules/ - Feature adapter module for customer opportunity center
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { computed, ref } from 'vue'

export function useCustomerOpportunityCenterData() {
  const mockMode = false
  const customerInsights = ref([])
  const customerPurchases = ref([])
  const customerPredictions = ref([])

  const salesUsers = computed(() => [])

  return {
    mockMode,
    customerInsights,
    customerPurchases,
    customerPredictions,
    salesUsers
  }
}
