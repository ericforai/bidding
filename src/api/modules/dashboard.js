/**
 * 数据看板与任务模块 API
 * 支持双模式切换，并在 API 模式下适配现有后端 analytics 契约
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'
import { buildFeatureUnavailableResponse } from '../featureAvailability.js'

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

function normalizeFilterValue(value, fallback = 'ALL') {
  if (value == null || String(value).trim() === '') return fallback
  return String(value).trim().toUpperCase()
}

function normalizeProjectStatusFilter(value) {
  const normalized = normalizeFilterValue(value)
  if (normalized === 'INPROGRESS') return 'IN_PROGRESS'
  return normalized
}

function getMockUserNameById(id) {
  return mockData.users.find((user) => String(user.id) === String(id))?.name || `用户#${id}`
}

function parseDateValue(value) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function isWithinDateRange(value, startDate, endDate) {
  if (!value) return false
  const date = parseDateValue(value)
  if (!date) return false
  const dateText = date.toISOString().slice(0, 10)
  if (startDate && dateText < startDate) return false
  if (endDate && dateText > endDate) return false
  return true
}

function sumRowAmounts(rows) {
  return rows.reduce((sum, row) => sum + Number(row?.amount || 0), 0)
}

function buildFilterDimension(key, label, selectedValue, rows, extractor, labelMap = {}) {
  const counts = rows.reduce((map, row) => {
    const value = extractor(row)
    if (!value) return map
    map.set(value, (map.get(value) || 0) + 1)
    return map
  }, new Map())

  return {
    key,
    label,
    selectedValue,
    options: [
      { label: '全部', value: 'ALL', count: rows.length },
      ...Array.from(counts.entries()).map(([value, count]) => ({
        label: labelMap[value] || value,
        value,
        count,
      })),
    ],
  }
}

function paginateRows(rows, page = 1, size = 10) {
  const normalizedPage = Math.max(Number(page) || 1, 1)
  const normalizedSize = Math.min(Math.max(Number(size) || 10, 1), 100)
  const start = (normalizedPage - 1) * normalizedSize
  const items = rows.slice(start, start + normalizedSize)
  const totalPages = rows.length === 0 ? 0 : Math.ceil(rows.length / normalizedSize)

  return {
    items,
    pagination: {
      page: normalizedPage,
      size: normalizedSize,
      total: rows.length,
      totalPages,
      hasNext: normalizedPage < totalPages,
    },
  }
}

function buildMockRevenueDrilldown(params = {}) {
  const selectedStatus = normalizeFilterValue(params.status)
  const rows = (mockData.tenders || [])
    .filter((tender) => isWithinDateRange(tender.date || tender.deadline, params.startDate, params.endDate))
    .map((tender) => {
      const relatedProject = (mockData.projects || []).find((project) => project.name.includes(tender.title.slice(0, 6)) || project.budget === tender.budget)
      const statusMap = {
        new: 'PENDING',
        following: 'TRACKING',
        bidding: 'BIDDED',
        abandoned: 'ABANDONED',
      }
      return {
        id: tender.id,
        relatedId: relatedProject?.id || null,
        title: tender.title,
        subtitle: tender.region || tender.industry || '未知来源',
        status: statusMap[tender.status] || 'PENDING',
        ownerName: relatedProject?.name || '未关联项目',
        amount: Number(tender.budget || 0),
        score: Number(tender.aiScore || 0),
        createdAt: tender.date ? `${tender.date}T00:00:00` : null,
        deadline: tender.deadline ? `${tender.deadline}T00:00:00` : null,
      }
    })
    .sort((a, b) => Number(b.amount || 0) - Number(a.amount || 0))

  const filteredRows = selectedStatus === 'ALL' ? rows : rows.filter((row) => row.status === selectedStatus)
  const { items, pagination } = paginateRows(filteredRows, params.page, params.size)

  return {
    metricKey: 'revenue',
    metricLabel: '中标金额明细',
    filters: {
      startDate: params.startDate || null,
      endDate: params.endDate || null,
      dimensions: [
        buildFilterDimension('status', '状态', selectedStatus, rows, (row) => row.status, {
          PENDING: '待处理',
          TRACKING: '跟踪中',
          BIDDED: '已投标',
          ABANDONED: '已放弃',
        }),
      ],
    },
    pagination,
    summary: {
      totalCount: filteredRows.length,
      totalAmount: sumRowAmounts(filteredRows),
    },
    items,
  }
}

function buildMockWinRateDrilldown(params = {}) {
  const selectedStatus = normalizeFilterValue(params.status)
  const rows = (mockData.tenders || [])
    .filter((tender) => isWithinDateRange(tender.date || tender.deadline, params.startDate, params.endDate))
    .map((tender) => {
      const relatedProject = (mockData.projects || []).find((project) => project.budget === tender.budget)
      let outcome = 'IN_PROGRESS'
      if (relatedProject?.status === 'won') outcome = 'WON'
      if (relatedProject?.status === 'lost' || tender.status === 'abandoned') outcome = 'LOST'
      if (tender.status === 'bidding') outcome = 'WON'

      return {
        id: tender.id,
        relatedId: relatedProject?.id || null,
        title: tender.title,
        subtitle: relatedProject?.name || '未形成项目',
        status: relatedProject?.status || tender.status,
        outcome,
        ownerName: relatedProject?.manager || '-',
        amount: Number(tender.budget || 0),
        rate: outcome === 'WON' ? 100 : 0,
        createdAt: tender.date ? `${tender.date}T00:00:00` : null,
        deadline: tender.deadline ? `${tender.deadline}T00:00:00` : null,
      }
    })

  const filteredRows = selectedStatus === 'ALL' ? rows : rows.filter((row) => row.outcome === selectedStatus)
  const wonCount = filteredRows.filter((row) => row.outcome === 'WON').length
  const { items, pagination } = paginateRows(filteredRows, params.page, params.size)

  return {
    metricKey: 'win-rate',
    metricLabel: '中标率明细',
    filters: {
      startDate: params.startDate || null,
      endDate: params.endDate || null,
      dimensions: [
        buildFilterDimension('outcome', '结果', selectedStatus, rows, (row) => row.outcome, {
          WON: '已中标',
          LOST: '未中标',
          IN_PROGRESS: '进行中',
        }),
      ],
    },
    pagination,
    summary: {
      totalCount: filteredRows.length,
      totalAmount: sumRowAmounts(filteredRows),
      wonCount,
      winRate: filteredRows.length > 0 ? Number(((wonCount / filteredRows.length) * 100).toFixed(1)) : 0,
    },
    items,
  }
}

function buildMockTeamDrilldown(params = {}) {
  const selectedRole = normalizeFilterValue(params.role)
  const userRoleMap = {
    admin: 'ADMIN',
    manager: 'MANAGER',
    staff: 'STAFF',
  }

  const rows = (mockData.users || []).map((user) => {
    const relatedProjects = (mockData.projects || []).filter((project) =>
      project.manager === user.name || (project.tasks || []).some((task) => task.owner === user.name)
    )
    const wonCount = relatedProjects.filter((project) => project.status === 'won').length
    const activeProjectCount = relatedProjects.filter((project) => project.status !== 'won' && project.status !== 'lost').length
    const managedProjectCount = relatedProjects.filter((project) => project.manager === user.name).length
    const relatedTasks = relatedProjects.flatMap((project) => (project.tasks || []).filter((task) => task.owner === user.name))
    const completedTaskCount = relatedTasks.filter((task) => task.status === 'done').length
    const overdueTaskCount = relatedTasks.filter((task) => task.status !== 'done' && task.deadline && task.deadline < '2026-03-11').length
    const totalTaskCount = relatedTasks.length
    const taskCompletionRate = totalTaskCount > 0 ? Number(((completedTaskCount / totalTaskCount) * 100).toFixed(1)) : 0
    const performanceScore = Math.round((relatedProjects.length > 0 ? (wonCount / relatedProjects.length) * 100 : 0) * 0.45 + taskCompletionRate * 0.4 + Math.max(0, 100 - (totalTaskCount > 0 ? (overdueTaskCount / totalTaskCount) * 100 : 0)) * 0.15)

    return {
      id: user.id,
      title: user.name,
      subtitle: user.dept || '-',
      role: userRoleMap[user.role] || 'STAFF',
      count: relatedProjects.length,
      wonCount,
      activeProjectCount,
      managedProjectCount,
      totalTaskCount,
      completedTaskCount,
      overdueTaskCount,
      rate: relatedProjects.length > 0 ? Number(((wonCount / relatedProjects.length) * 100).toFixed(1)) : 0,
      taskCompletionRate,
      amount: relatedProjects.reduce((sum, project) => sum + Number(project.budget || 0), 0),
      score: performanceScore,
      teamSize: managedProjectCount,
    }
  }).sort((a, b) => Number(b.score || 0) - Number(a.score || 0))

  const filteredRows = selectedRole === 'ALL' ? rows : rows.filter((row) => row.role === selectedRole)
  const { items, pagination } = paginateRows(filteredRows, params.page, params.size)

  return {
    metricKey: 'team',
    metricLabel: '人员绩效明细',
    filters: {
      startDate: params.startDate || null,
      endDate: params.endDate || null,
      dimensions: [
        buildFilterDimension('role', '角色', selectedRole, rows, (row) => row.role, {
          ADMIN: '管理员',
          MANAGER: '经理',
          STAFF: '员工',
        }),
      ],
    },
    pagination,
    summary: {
      totalCount: filteredRows.length,
      totalAmount: sumRowAmounts(filteredRows),
      totalTeamMembers: filteredRows.length,
      totalCompletedTasks: filteredRows.reduce((sum, row) => sum + Number(row.completedTaskCount || 0), 0),
      totalOverdueTasks: filteredRows.reduce((sum, row) => sum + Number(row.overdueTaskCount || 0), 0),
      winRate: filteredRows.length > 0
        ? Number((filteredRows.reduce((sum, row) => sum + Number(row.rate || 0), 0) / filteredRows.length).toFixed(1))
        : 0,
      averageTaskCompletionRate: filteredRows.length > 0
        ? Number((filteredRows.reduce((sum, row) => sum + Number(row.taskCompletionRate || 0), 0) / filteredRows.length).toFixed(1))
        : 0,
    },
    items,
  }
}

function buildMockProjectsDrilldown(params = {}) {
  const selectedStatus = normalizeProjectStatusFilter(params.status)
  const statusMap = {
    bidding: 'BIDDING',
    reviewing: 'REVIEWING',
    preparing: 'PREPARING',
    initiated: 'INITIATED',
    won: 'ARCHIVED',
    lost: 'ARCHIVED',
  }

  const rows = (mockData.projects || [])
    .filter((project) => isWithinDateRange(project.createTime || project.deadline, params.startDate, params.endDate))
    .map((project) => ({
      id: project.id,
      relatedId: null,
      title: project.name,
      subtitle: project.customer || '-',
      status: statusMap[project.status] || 'PREPARING',
      ownerName: project.manager || '-',
      amount: Number(project.budget || 0),
      teamSize: (project.tasks || []).length,
      createdAt: project.createTime ? `${project.createTime}T00:00:00` : null,
      deadline: project.deadline ? `${project.deadline}T00:00:00` : null,
    }))
    .sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))

  const filteredRows = rows.filter((row) => {
    if (selectedStatus === 'ALL') return true
    if (selectedStatus === 'IN_PROGRESS') return row.status !== 'ARCHIVED'
    return row.status === selectedStatus
  })
  const { items, pagination } = paginateRows(filteredRows, params.page, params.size)

  return {
    metricKey: 'projects',
    metricLabel: '进行中项目明细',
    filters: {
      startDate: params.startDate || null,
      endDate: params.endDate || null,
      dimensions: [
        {
          key: 'status',
          label: '项目状态',
          selectedValue: selectedStatus,
          options: [
            { label: '全部', value: 'ALL', count: rows.length },
            { label: '进行中', value: 'IN_PROGRESS', count: rows.filter((row) => row.status !== 'ARCHIVED').length },
            ...['INITIATED', 'PREPARING', 'REVIEWING', 'BIDDING', 'ARCHIVED'].map((value) => ({
              label: {
                INITIATED: '已启动',
                PREPARING: '准备中',
                REVIEWING: '审核中',
                BIDDING: '投标中',
                ARCHIVED: '已归档',
              }[value],
              value,
              count: rows.filter((row) => row.status === value).length,
            })),
          ],
        },
      ],
    },
    pagination,
    summary: {
      totalCount: filteredRows.length,
      totalAmount: sumRowAmounts(filteredRows),
      activeCount: filteredRows.filter((row) => row.status !== 'ARCHIVED').length,
    },
    items,
  }
}

function buildMockDrillDown(type, params = {}) {
  switch (type) {
    case 'revenue':
      return buildMockRevenueDrilldown(params)
    case 'win-rate':
      return buildMockWinRateDrilldown(params)
    case 'team':
      return buildMockTeamDrilldown(params)
    case 'projects':
      return buildMockProjectsDrilldown(params)
    default:
      return {
        metricKey: type,
        metricLabel: '明细',
        filters: { startDate: params.startDate || null, endDate: params.endDate || null, dimensions: [] },
        pagination: { page: 1, size: 10, total: 0, totalPages: 0, hasNext: false },
        summary: { totalCount: 0, totalAmount: 0 },
        items: [],
      }
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

function unsupportedApiResponse(message, data = []) {
  return buildFeatureUnavailableResponse({
    feature: 'unsupported-api-action',
    title: '当前操作暂未接入',
    message,
    hint: '请继续使用已接入的列表、详情和创建流程。',
    scope: 'action',
    data,
  })
}

export const dashboardApi = {
  async getOverview() {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: buildMockOverview() })
    }

    const response = await httpClient.get('/api/analytics/overview')
    return { ...response, data: buildApiOverview(response?.data) }
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
    return { ...response, data: apiData }
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
    return {
      ...response,
      data: competitors.map((item) => normalizeCompetitorItem(item, totalAmount)),
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
    return { ...response, data: apiData }
  },

  async getProductLines() {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: mockData.dashboard?.productLines || [],
      })
    }

    return Promise.resolve({
      ...buildFeatureUnavailableResponse({
        feature: 'analytics-product-lines',
        title: '产品线分析暂未接入',
        message: 'Product line analytics are not implemented on the backend yet',
        hint: '当前页其余指标仍基于真实后端数据加载。',
        scope: 'section',
        data: [],
      }),
    })
  },

  async getDrillDown(type, params = {}) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: buildMockDrillDown(type, params),
      })
    }

    const endpointMap = {
      revenue: '/api/analytics/drilldown/revenue',
      'win-rate': '/api/analytics/drilldown/win-rate',
      team: '/api/analytics/drilldown/team',
      projects: '/api/analytics/drilldown/projects',
    }
    const endpoint = endpointMap[type]
    if (!endpoint) {
      return Promise.resolve({
        ...buildFeatureUnavailableResponse({
          feature: `analytics-drilldown-${type}`,
          title: '下钻类型暂不支持',
          message: `Unsupported drill-down type: ${type}`,
          hint: '请从已接入的指标卡片进入真实明细。',
          scope: 'drawer',
          data: [],
        }),
      })
    }

    return httpClient.get(endpoint, { params })
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
    return Promise.resolve(
      unsupportedApiResponse('Task completion shortcut is not aligned with the backend contract yet')
    )
  },
}

export const todosApi = {
  async getList() {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: mockData.todos })
    }
    return Promise.resolve(unsupportedApiResponse('Todo endpoints are not implemented on the backend yet'))
  },

  async complete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { id, status: 'completed' } })
    }
    return Promise.resolve(unsupportedApiResponse('Todo endpoints are not implemented on the backend yet'))
  },
}

export default {
  dashboard: dashboardApi,
  tasks: tasksApi,
  todos: todosApi,
}
