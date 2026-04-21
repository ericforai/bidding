// Input: customer opportunity refs, filters, and selected-customer context
// Output: pure view-model helpers for CustomerOpportunityCenter
// Pos: src/views/Bidding/ - Customer opportunity view utility

export const CUSTOMER_OPPORTUNITY_STATUS_OPTIONS = [
  { label: '待判断机会', value: 'watch' },
  { label: '建议转项目', value: 'recommend' },
  { label: '已转化项目', value: 'converted' }
]

export const EMPTY_CUSTOMER_PREDICTION = Object.freeze({
  opportunityId: '',
  suggestedProjectName: '待智能研判',
  predictedCategory: '---',
  predictedBudgetMin: 0,
  predictedBudgetMax: 0,
  predictedWindow: '待判断',
  confidence: 0,
  reasoningSummary: '当前数据不足，暂无法生成高置信度预测。',
  evidenceRecords: [],
  convertedProjectId: ''
})

const CATEGORY_COLORS = ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#64748b']

export function normalizeConfidence(score) {
  return Math.max(0, Math.min(100, Math.round(Number(score || 0) * 100)))
}

export function filterCustomers(customers = [], filters = {}) {
  return customers.filter((customer) => {
    if (filters.status && customer.status !== filters.status) {
      return false
    }
    if (filters.keyword && !customer.customerName.toLowerCase().includes(String(filters.keyword).toLowerCase())) {
      return false
    }
    if (filters.sales && customer.salesRep !== filters.sales) {
      return false
    }
    if (filters.region && customer.region !== filters.region) {
      return false
    }
    if (filters.industry && customer.industry !== filters.industry) {
      return false
    }
    return true
  })
}

export function buildSelectedCustomer(customerInsights = [], customerPurchases = [], customerPredictions = [], activeCustomerId = '') {
  const baseCustomer = customerInsights.find((item) => item.customerId === activeCustomerId)
  if (!baseCustomer) {
    return null
  }

  const purchaseHistory = customerPurchases.filter((item) => item.customerId === baseCustomer.customerId)
  const prediction = customerPredictions.find((item) => item.customerId === baseCustomer.customerId)

  return {
    ...baseCustomer,
    purchaseHistory,
    prediction: {
      ...EMPTY_CUSTOMER_PREDICTION,
      ...(prediction || {})
    }
  }
}

export function buildCustomerHistory(selectedCustomer, customerPurchases = []) {
  if (!selectedCustomer) {
    return []
  }

  return customerPurchases
    .filter((item) => item.customerId === selectedCustomer.customerId)
    .sort((a, b) => new Date(b.publishDate) - new Date(a.publishDate))
}

export function buildDrawerStats(history = []) {
  const totalCount = history.length
  const totalBudget = history.reduce((sum, item) => sum + (item.budget || 0), 0)
  const categoryCounter = history.reduce((acc, item) => {
    acc[item.category] = (acc[item.category] || 0) + 1
    return acc
  }, {})
  const topCategory = Object.entries(categoryCounter).sort((a, b) => b[1] - a[1])[0]?.[0] || '未知'

  return {
    totalCount,
    totalBudget,
    topCategory
  }
}

export function buildCategoryStats(history = []) {
  const total = history.length
  if (!total) {
    return []
  }

  const categoryCounter = history.reduce((acc, item) => {
    acc[item.category] = (acc[item.category] || 0) + 1
    return acc
  }, {})

  return Object.entries(categoryCounter)
    .sort((a, b) => b[1] - a[1])
    .map(([name, count], index) => ({
      name,
      count,
      percent: Math.round((count / total) * 100),
      color: CATEGORY_COLORS[index % CATEGORY_COLORS.length]
    }))
}

export function buildBoardSummaries({ demoEnabled, customerInsights = [], customerPredictions = [] }) {
  if (!demoEnabled) {
    return [
      { label: '客户池', value: '--', note: '真实客户数据源未接入', tag: '未接入', tagType: 'info', placeholder: true, trendLabel: 'API' },
      { label: '采购记录', value: '--', note: '历史采购服务待真实数据源接入', tag: '未接入', tagType: 'info', placeholder: true, trendLabel: 'API' },
      { label: '预测商机', value: '--', note: '预测结果不会在真实模式下伪造', tag: '未接入', tagType: 'info', placeholder: true, trendLabel: 'API' },
      { label: '项目转化', value: '--', note: '转项目链路待真实数据源接入', tag: '未接入', tagType: 'info', placeholder: true, trendLabel: 'API' }
    ]
  }

  const highValueCount = customerInsights.filter((item) => item.opportunityScore >= 85).length
  const shortTermCount = customerPredictions.filter((item) => /^2025-0[3-4]/.test(item.predictedWindow)).length
  const midTermCount = customerPredictions.filter((item) => /^2025-0[5-6]/.test(item.predictedWindow)).length
  const convertedCount = customerPredictions.filter((item) => item.convertedProjectId).length

  return [
    { label: '高价值客户', value: String(highValueCount), note: '核心经营资产', tag: '重点', tagType: 'success', trend: 12, isUp: true },
    { label: '30D 预测机会', value: String(shortTermCount), note: '需近期重点研判', tag: '紧迫', tagType: 'danger', trend: 8, isUp: true },
    { label: '远期潜客', value: String(midTermCount), note: '适合关系铺垫', tag: '观察', tagType: 'warning', trend: 3, isUp: false },
    { label: '已转化', value: String(convertedCount), note: '已转正式项目池', tag: '完成', tagType: 'info', trend: 20, isUp: true }
  ]
}

export function getScoreColor(score) {
  return score >= 80 ? '#10b981' : score >= 60 ? '#f59e0b' : '#64748b'
}

export function getScoreClass(score) {
  return score >= 80 ? 'high' : score >= 60 ? 'mid' : 'low'
}

export function getStatusLabel(status) {
  const statusMap = {
    watch: '待研判',
    recommend: '商机推荐',
    converted: '已立项'
  }
  return statusMap[status] || '待研判'
}

export function getStatusType(status) {
  const statusTypeMap = {
    watch: 'info',
    recommend: 'success',
    converted: 'warning'
  }
  return statusTypeMap[status] || 'info'
}

export function confidenceColor(value) {
  return value >= 80 ? '#10b981' : value >= 60 ? '#f59e0b' : '#3b82f6'
}

export function buildDeadlineFromWindow(windowValue) {
  if (!windowValue) {
    return ''
  }
  if (/^\d{4}-\d{2}$/.test(windowValue)) {
    return `${windowValue}-28`
  }
  return ''
}

export function buildCreateProjectQuery(selectedCustomer) {
  if (!selectedCustomer) {
    return {}
  }

  const averageBudget = Math.round(
    (Number(selectedCustomer.prediction?.predictedBudgetMin || 0) +
      Number(selectedCustomer.prediction?.predictedBudgetMax || 0)) / 2
  )

  return {
    projectName: selectedCustomer.prediction?.suggestedProjectName || '',
    customerName: selectedCustomer.customerName || '',
    industry: selectedCustomer.industry || '',
    region: selectedCustomer.region || '',
    budget: String(averageBudget),
    deadline: buildDeadlineFromWindow(selectedCustomer.prediction?.predictedWindow),
    tags: Array.isArray(selectedCustomer.mainCategories) ? selectedCustomer.mainCategories.join(',') : '',
    description: `基于历史采购规律预测，建议围绕“${selectedCustomer.prediction?.predictedCategory || ''}”提前立项跟进。`,
    remark: `预测时间窗口：${selectedCustomer.prediction?.predictedWindow || '待判断'}；置信度：${normalizeConfidence(selectedCustomer.prediction?.confidence)}%`,
    sourceModule: 'customer-opportunity-center',
    sourceCustomerId: selectedCustomer.customerId || '',
    sourceCustomerName: selectedCustomer.customerName || '',
    sourceOpportunityId: selectedCustomer.prediction?.opportunityId || '',
    sourceReasoningSummary: selectedCustomer.prediction?.reasoningSummary || ''
  }
}

export function resolveCustomerOpportunityViewState({ loading = false, demoEnabled = false, selectedCustomer = null } = {}) {
  return {
    showLoading: loading,
    showDetail: !loading && Boolean(selectedCustomer),
    showOnboarding: !loading && demoEnabled && !selectedCustomer,
    showApiEmpty: !loading && !demoEnabled && !selectedCustomer
  }
}
