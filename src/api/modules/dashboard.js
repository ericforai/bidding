/**
 * 数据看板与任务模块 API
 * 支持双模式切换，并在 API 模式下适配现有后端 analytics 契约
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

function formatChange(change) {
  if (change == null) return '--'
  const numeric = Number(change)
  if (Number.isNaN(numeric)) return '--'
  return `${numeric >= 0 ? '+' : ''}${numeric.toFixed(1)}%`
}

function normalizeTrendItem(item) {
  return {
    month: item?.period || '-',
    bids: Number(item?.count || 0),
    wins: 0,
    rate: Number(item?.changePercentage || 0),
    amount: Number(item?.value || 0),
  }
}

function normalizeCompetitorItem(item, totalAmount) {
  const amount = Number(item?.totalBidAmount || 0)
  return {
    name: item?.name || '未知竞争对手',
    share: totalAmount > 0 ? Number(((amount / totalAmount) * 100).toFixed(1)) : 0,
    amount,
    bids: Number(item?.bidCount || 0),
    wins: Number(item?.winCount || 0),
    rate: Number(item?.winRate || 0),
  }
}

function normalizeRegionItem(item) {
  return {
    name: item?.region || '未知区域',
    amount: Number(item?.totalBudget || 0),
    bids: Number(item?.tenderCount || 0),
    rate: Number(item?.percentage || 0),
  }
}

function buildMockOverview() {
  return {
    ...mockData.dashboard,
    productLines: mockData.dashboard?.productLines || [],
  }
}

function buildApiOverview(overview = {}) {
  const summary = overview?.summaryStats || {}
  const competitors = Array.isArray(overview?.topCompetitors) ? overview.topCompetitors : []
  const competitorTotalAmount = competitors.reduce((sum, item) => sum + Number(item?.totalBidAmount || 0), 0)

  return {
    totalBids: Number(summary?.totalTenders || 0),
    totalBidsChange: '--',
    inProgress: Number(summary?.activeProjects || 0),
    wonThisYear: 0,
    winRate: Number(summary?.successRate || 0),
    winRateChange: '--',
    totalAmount: Number(summary?.totalBudget || 0),
    totalAmountChange: '--',
    totalCost: 0,
    totalCostChange: '--',
    trendData: Array.isArray(overview?.tenderTrends) ? overview.tenderTrends.map(normalizeTrendItem) : [],
    competitors: competitors.map((item) => normalizeCompetitorItem(item, competitorTotalAmount)),
    productLines: [],
    regionData: Array.isArray(overview?.regionalDistribution) ? overview.regionalDistribution.map(normalizeRegionItem) : [],
    statusDistribution: overview?.statusDistribution || {},
    backendSummary: {
      activeProjects: Number(summary?.activeProjects || 0),
      pendingTasks: Number(summary?.pendingTasks || 0),
    },
  }
}

function hasApiOverviewData(overview = {}) {
  const summary = overview?.summaryStats || {}
  const summaryValues = [
    Number(summary?.totalTenders || 0),
    Number(summary?.activeProjects || 0),
    Number(summary?.successRate || 0),
    Number(summary?.totalBudget || 0),
    Number(summary?.pendingTasks || 0),
  ]

  return summaryValues.some((value) => value > 0) ||
    (Array.isArray(overview?.tenderTrends) && overview.tenderTrends.length > 0) ||
    (Array.isArray(overview?.topCompetitors) && overview.topCompetitors.length > 0) ||
    (Array.isArray(overview?.regionalDistribution) && overview.regionalDistribution.length > 0)
}

export const dashboardApi = {
  async getOverview() {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: buildMockOverview() })
    }

    const response = await httpClient.get('/api/analytics/overview')
    const data = hasApiOverviewData(response?.data) ? buildApiOverview(response?.data) : buildMockOverview()
    return {
      ...response,
      data,
    }
  },

  async getStats() {
    return this.getOverview()
  },

  async getTrend() {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.dashboard?.trendData || [],
      })
    }

    const response = await httpClient.get('/api/analytics/trends')
    const apiData = Array.isArray(response?.data?.tenders) ? response.data.tenders.map(normalizeTrendItem) : []
    return {
      ...response,
      data: apiData.length > 0 ? apiData : (mockData.dashboard?.trendData || []),
    }
  },

  async getCompetitors() {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.dashboard?.competitors || [],
      })
    }

    const response = await httpClient.get('/api/analytics/competitors')
    const competitors = Array.isArray(response?.data) ? response.data : []
    const totalAmount = competitors.reduce((sum, item) => sum + Number(item?.totalBidAmount || 0), 0)
    const data = competitors.length > 0
      ? competitors.map((item) => normalizeCompetitorItem(item, totalAmount))
      : (mockData.dashboard?.competitors || [])

    return {
      ...response,
      data,
    }
  },

  async getRegionData() {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.dashboard?.regionData || [],
      })
    }

    const response = await httpClient.get('/api/analytics/regions')
    const apiData = Array.isArray(response?.data) ? response.data.map(normalizeRegionItem) : []
    return {
      ...response,
      data: apiData.length > 0 ? apiData : (mockData.dashboard?.regionData || []),
    }
  },

  async getProductLines() {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.dashboard?.productLines || [],
      })
    }

    return Promise.resolve({
      success: true,
      message: '使用演示产品线数据',
      data: mockData.dashboard?.productLines || [],
    })
  },
}

export const tasksApi = {
  async getList(params) {
    if (isMockMode()) {
      let tasks = []
      mockData.projects.forEach((project) => {
        if (project.tasks) {
          tasks.push(...project.tasks.map((task) => ({ ...task, projectName: project.name })))
        }
      })
      if (params?.status) {
        tasks = tasks.filter((task) => task.status === params.status)
      }
      if (params?.priority) {
        tasks = tasks.filter((task) => task.priority === params.priority)
      }
      return Promise.resolve({ success: true, data: tasks, total: tasks.length })
    }
    return httpClient.get('/api/tasks', { params })
  },

  async getDetail(id) {
    if (isMockMode()) {
      for (const project of mockData.projects) {
        if (project.tasks) {
          const task = project.tasks.find((item) => item.id === id)
          if (task) {
            return Promise.resolve({ success: true, data: { ...task, projectName: project.name } })
          }
        }
      }
      return Promise.resolve({ success: false, message: '任务不存在' })
    }
    return httpClient.get(`/api/tasks/${id}`)
  },

  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: { ...data, id: `TK${Date.now()}`, createTime: new Date().toISOString() },
      })
    }
    return httpClient.post('/api/tasks', data)
  },

  async update(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { ...data, id } })
    }
    return httpClient.put(`/api/tasks/${id}`, data)
  },

  async delete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    return httpClient.delete(`/api/tasks/${id}`)
  },

  async complete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { id, status: 'done' } })
    }
    return Promise.resolve({
      success: false,
      message: 'Task completion shortcut is not aligned with the backend contract yet',
    })
  },
}

export const todosApi = {
  async getList() {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: mockData.todos })
    }
    return Promise.resolve({
      success: false,
      message: 'Todo endpoints are not implemented on the backend yet',
      data: [],
    })
  },

  async complete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { id, status: 'completed' } })
    }
    return Promise.resolve({
      success: false,
      message: 'Todo endpoints are not implemented on the backend yet',
    })
  },
}

export default {
  dashboard: dashboardApi,
  tasks: tasksApi,
  todos: todosApi,
}
