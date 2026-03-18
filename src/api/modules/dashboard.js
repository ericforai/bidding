// Input: httpClient, API mode config, analytics normalizers and demo adapters
// Output: dashboardApi - dashboard metrics, tasks, and drill-down accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 数据看板与任务模块 API
 * 支持双模式切换，并在 API 模式下适配现有后端 analytics 契约
 */
import { mockData } from '../mock.js'
import httpClient from '../client.js'
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

function normalizeProductLineItem(item) {
  return {
    name: item?.name || '综合解决方案',
    revenue: Number(item?.revenue || 0),
    cost: Number(item?.cost || 0),
    bids: Number(item?.bids || 0),
    rate: Number(item?.rate || 0),
  }
}

function buildMockOverview() {
  return {
    ...mockData.dashboard,
    productLines: mockData.dashboard?.productLines || [],
  }
}

function normalizeMockProjectStatus(status) {
  const value = String(status || '').toLowerCase()
  const map = {
    pending: 'INITIATED',
    drafting: 'PREPARING',
    reviewing: 'REVIEWING',
    bidding: 'BIDDING',
    won: 'ARCHIVED',
    lost: 'ARCHIVED',
  }
  return map[value] || 'PREPARING'
}

function buildMockMetricDimensions(type) {
  if (type === 'projects') {
    return [
      {
        key: 'status',
        label: '项目状态',
        options: [
          { label: '全部', value: 'ALL' },
          { label: '已启动', value: 'INITIATED' },
          { label: '准备中', value: 'PREPARING' },
          { label: '审核中', value: 'REVIEWING' },
          { label: '投标中', value: 'BIDDING' },
          { label: '已归档', value: 'ARCHIVED' },
        ],
      },
    ]
  }

  if (type === 'team') {
    return [
      {
        key: 'role',
        label: '角色',
        options: [
          { label: '全部', value: 'ALL' },
          { label: '管理层', value: 'ADMIN' },
          { label: '经理', value: 'MANAGER' },
          { label: '员工', value: 'STAFF' },
        ],
      },
    ]
  }

  if (type === 'win-rate') {
    return [
      {
        key: 'outcome',
        label: '结果',
        options: [
          { label: '全部', value: 'ALL' },
          { label: '已中标', value: 'WON' },
          { label: '未中标', value: 'LOST' },
          { label: '进行中', value: 'IN_PROGRESS' },
        ],
      },
    ]
  }

  return []
}

function paginateMockItems(items, params = {}) {
  const page = Math.max(1, Number(params?.page || 1))
  const size = Math.max(1, Number(params?.size || 10))
  const total = items.length
  const totalPages = Math.ceil(total / size)
  const start = (page - 1) * size
  const paged = items.slice(start, start + size)

  return {
    items: paged,
    pagination: {
      page,
      size,
      total,
      totalPages,
      hasNext: page < totalPages,
    },
  }
}

function buildMockDrillDown(type, params = {}) {
  const tenders = Array.isArray(mockData.tenders) ? mockData.tenders : []
  const projects = Array.isArray(mockData.projects) ? mockData.projects : []
  const users = Array.isArray(mockData.users) ? mockData.users : []

  if (type === 'revenue') {
    let items = tenders.map((tender) => ({
      id: tender.id,
      title: tender.title,
      subtitle: `${tender.source || '内部'} / ${tender.region || '-'}`,
      status: String(tender.status || 'new').toLowerCase(),
      ownerName: projects.find((project) => String(project.name).includes(String(tender.title).slice(0, 6)))?.name || '待关联项目',
      score: Number(tender.aiScore || 0),
      amount: Number(tender.budget || 0),
      createdAt: tender.date || '',
      deadline: tender.deadline || tender.date || '',
      relatedId: tender.id,
    })).sort((a, b) => b.amount - a.amount)

    const { items: paged, pagination } = paginateMockItems(items, params)
    return {
      items: paged,
      summary: {
        totalCount: items.length,
        totalAmount: items.reduce((sum, item) => sum + item.amount, 0),
      },
      filters: { dimensions: buildMockMetricDimensions(type) },
      pagination,
    }
  }

  if (type === 'win-rate') {
    const outcomes = ['WON', 'LOST', 'IN_PROGRESS']
    let items = tenders.map((tender, index) => ({
      id: tender.id,
      title: tender.title,
      subtitle: projects[index % Math.max(projects.length, 1)]?.name || '待关联项目',
      outcome: outcomes[index % outcomes.length],
      ownerName: projects[index % Math.max(projects.length, 1)]?.manager || '未分配',
      amount: Number(tender.budget || 0),
      rate: Number(tender.probability === 'high' ? 78 : tender.probability === 'medium' ? 52 : 31),
      createdAt: tender.date || '',
      relatedId: tender.id,
    }))

    if (params?.role && params.role !== 'ALL') {
      items = items.filter((item) => item.outcome === params.role)
    }

    const { items: paged, pagination } = paginateMockItems(items, params)
    return {
      items: paged,
      summary: {
        totalCount: items.length,
        totalAmount: items.reduce((sum, item) => sum + item.amount, 0),
      },
      filters: { dimensions: buildMockMetricDimensions(type) },
      pagination,
    }
  }

  if (type === 'team') {
    let items = users.map((user) => {
      const relatedProjects = projects.filter((project) =>
        project.manager === user.name || (project.tasks || []).some((task) => task.owner === user.name)
      )
      const wonCount = relatedProjects.filter((project) => project.status === 'won').length
      const activeProjectCount = relatedProjects.filter((project) => ['drafting', 'reviewing', 'bidding'].includes(project.status)).length
      const managedProjectCount = relatedProjects.filter((project) => project.manager === user.name).length
      const relatedTasks = relatedProjects.flatMap((project) => (project.tasks || []).filter((task) => task.owner === user.name))
      const completedTaskCount = relatedTasks.filter((task) => task.status === 'done').length
      const overdueTaskCount = relatedTasks.filter((task) => task.status !== 'done' && task.deadline && task.deadline < '2026-03-11').length
      const totalTaskCount = relatedTasks.length
      const taskCompletionRate = totalTaskCount > 0 ? Number(((completedTaskCount / totalTaskCount) * 100).toFixed(1)) : 0
      const winRate = relatedProjects.length > 0 ? Number(((wonCount / relatedProjects.length) * 100).toFixed(1)) : 0
      const amount = relatedProjects.reduce((sum, project) => sum + Number(project.budget || 0), 0)
      const role = String(user.role || 'staff').toUpperCase()
      const performanceScore = Math.round(
        winRate * 0.45 +
        taskCompletionRate * 0.4 +
        Math.max(0, 100 - (totalTaskCount > 0 ? (overdueTaskCount / totalTaskCount) * 100 : 0)) * 0.15
      )

      return {
        id: user.id,
        title: user.name,
        subtitle: user.dept || '-',
        role,
        count: relatedProjects.length,
        wonCount,
        activeProjectCount,
        managedProjectCount,
        totalTaskCount,
        completedTaskCount,
        overdueTaskCount,
        taskCompletionRate,
        rate: winRate,
        score: performanceScore,
        amount,
      }
    }).sort((a, b) => Number(b.score || 0) - Number(a.score || 0))

    if (params?.role && params.role !== 'ALL') {
      items = items.filter((item) => item.role === params.role)
    }

    const { items: paged, pagination } = paginateMockItems(items, params)
    return {
      metricLabel: '人员绩效明细',
      items: paged,
      summary: {
        totalCount: items.length,
        totalAmount: items.reduce((sum, item) => sum + item.amount, 0),
        totalTeamMembers: items.length,
        totalCompletedTasks: items.reduce((sum, item) => sum + Number(item.completedTaskCount || 0), 0),
        totalOverdueTasks: items.reduce((sum, item) => sum + Number(item.overdueTaskCount || 0), 0),
        winRate: items.length > 0 ? Number((items.reduce((sum, item) => sum + Number(item.rate || 0), 0) / items.length).toFixed(1)) : 0,
        averageTaskCompletionRate: items.length > 0 ? Number((items.reduce((sum, item) => sum + Number(item.taskCompletionRate || 0), 0) / items.length).toFixed(1)) : 0,
      },
      filters: { dimensions: buildMockMetricDimensions(type) },
      pagination,
    }
  }

  let items = projects.map((project) => ({
    id: project.id,
    title: project.name,
    subtitle: `${project.customer || '-'} / ${project.industry || '-'}`,
    status: normalizeMockProjectStatus(project.status),
    ownerName: project.manager || '未分配',
    teamSize: Array.isArray(project.tasks) ? new Set(project.tasks.map((task) => task.owner).filter(Boolean)).size : 0,
    amount: Number(project.budget || 0),
    createdAt: project.createTime || '',
    deadline: project.deadline || '',
    relatedId: project.id,
  }))

  if (params?.status && params.status !== 'ALL') {
    items = items.filter((item) => item.status === params.status)
  }

  const { items: paged, pagination } = paginateMockItems(items, params)
  return {
    items: paged,
    summary: {
      totalCount: items.length,
      totalAmount: items.reduce((sum, item) => sum + item.amount, 0),
    },
    filters: { dimensions: buildMockMetricDimensions('projects') },
    pagination,
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

export const dashboardApi = {
  async getOverview() {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: buildMockOverview() })
    }

    const [overviewResponse, productLineResponse] = await Promise.all([
      httpClient.get('/api/analytics/overview'),
      httpClient.get('/api/analytics/product-lines'),
    ])

    const data = {
      ...buildApiOverview(overviewResponse?.data),
      productLines: Array.isArray(productLineResponse?.data)
        ? productLineResponse.data.map(normalizeProductLineItem)
        : [],
    }

    return {
      ...overviewResponse,
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
      data: apiData,
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
    const data = competitors.map((item) => normalizeCompetitorItem(item, totalAmount))

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
      data: apiData,
    }
  },

  async getProductLines() {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.dashboard?.productLines || [],
      })
    }

    const response = await httpClient.get('/api/analytics/product-lines')
    const apiData = Array.isArray(response?.data) ? response.data.map(normalizeProductLineItem) : []
    return {
      ...response,
      data: apiData,
    }
  },

  async getDrillDown(type, key) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: buildMockDrillDown(type, key),
      })
    }

    return httpClient.get('/api/analytics/drill-down', {
      params: { type, key },
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
