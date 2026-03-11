/**
 * 资源管理模块 API
 * 支持双模式切换，并在 API 模式下对齐当前后端契约
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

function isNumericId(id) {
  return /^\d+$/.test(String(id))
}

function invalidIdMessage(entityName) {
  return {
    success: false,
    message: `Current backend only supports numeric ${entityName} IDs in API mode`,
  }
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toISOString().split('T')[0]
}

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

function normalizeAccountStatus(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'AVAILABLE') return 'available'
  if (value === 'BORROWED' || value === 'IN_USE') return 'in_use'
  if (value === 'DISABLED') return 'disabled'
  return 'available'
}

function normalizePlatformLabel(platformType, fallback) {
  const type = String(platformType || '').toUpperCase()
  const map = {
    BID_PLATFORM: '投标平台',
    PROCUREMENT_PLATFORM: '采购平台',
    GOVERNMENT_PLATFORM: '政府平台',
    OTHER: '其他平台',
  }
  return fallback || map[type] || type || '未知平台'
}

function normalizeAccount(item = {}) {
  return {
    id: item.id,
    platform: item.platform || normalizePlatformLabel(item.platformType, item.accountName),
    username: item.username || '',
    password: item.password || '',
    status: item.status ? normalizeAccountStatus(item.status) : 'available',
    lastUsed: formatDateTime(item.updatedAt || item.borrowedAt || item.lastUsed),
    borrower: item.borrower || (item.borrowedBy ? `用户#${item.borrowedBy}` : ''),
    dueAt: formatDateTime(item.dueAt),
    raw: item,
  }
}

function normalizeExpenseCategory(category) {
  const value = String(category || '').toUpperCase()
  const map = {
    MATERIAL: '其他',
    LABOR: '其他',
    EQUIPMENT: '其他',
    TRANSPORTATION: '差旅费',
    SUBCONTRACTING: '其他',
    OVERHEAD: '其他',
    OTHER: '其他',
  }
  return map[value] || category || '其他'
}

function normalizeExpense(item = {}) {
  const backendStatus = String(item.status || '').toUpperCase()
  let status = item.status || 'paid'
  let approvalStatus = item.approvalStatus || 'approved'

  if (backendStatus === 'PENDING_APPROVAL') {
    status = 'pending'
    approvalStatus = 'pending'
  } else if (backendStatus === 'APPROVED') {
    status = 'pending'
    approvalStatus = 'approved'
  } else if (backendStatus === 'REJECTED') {
    status = 'pending'
    approvalStatus = 'rejected'
  } else if (backendStatus === 'PAID') {
    status = 'paid'
    approvalStatus = 'approved'
  } else if (backendStatus === 'RETURN_REQUESTED') {
    status = 'paid'
    approvalStatus = 'approved'
  } else if (backendStatus === 'RETURNED') {
    status = 'returned'
    approvalStatus = 'approved'
  }

  return {
    id: item.id,
    project: item.project || item.projectName || (item.projectId ? `项目#${item.projectId}` : '未关联项目'),
    projectId: item.projectId,
    type: item.type || item.expenseType || normalizeExpenseCategory(item.category),
    amount: Number(item.amount || 0),
    status,
    approvalStatus,
    backendStatus,
    date: item.date || formatDate(item.createdAt),
    returnDate: item.returnDate || '',
    returnRequestedAt: item.returnRequestedAt || '',
    returnConfirmedAt: item.returnConfirmedAt || '',
    description: item.description || '',
    createdBy: item.createdBy || '',
    approvedBy: item.approvedBy || '',
    approvedAt: item.approvedAt || '',
    approvalComment: item.approvalComment || '',
    returnComment: item.returnComment || '',
    raw: item,
  }
}

function normalizeExpenseMutationResponse(response) {
  return {
    ...response,
    data: response?.data ? normalizeExpense(response.data) : response?.data,
  }
}

function parseBarMeta(remark) {
  if (!remark || typeof remark !== 'string') return {}
  if (!remark.startsWith('BAR_SITE_META:')) return {}

  try {
    const meta = JSON.parse(remark.slice('BAR_SITE_META:'.length))
    return {
      url: meta.u || '',
      region: meta.r || '',
      industry: meta.i || '',
      siteType: meta.s || '',
      loginType: meta.l || '',
      remark: meta.m || '',
      lastVerifyTime: meta.v || '',
    }
  } catch {
    return {}
  }
}

function createBarRemark(site = {}) {
  const meta = {
    u: site.url || '',
    r: site.region || '',
    i: site.industry || '',
    s: site.siteType || '',
    l: site.loginType || '',
    m: site.remark || '',
    v: site.lastVerifyTime || '',
  }

  return `BAR_SITE_META:${JSON.stringify(meta)}`
}

function normalizeBarStatus(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'AVAILABLE' || value === 'IN_USE') return 'active'
  return 'inactive'
}

function normalizeRiskLevel(asset = {}) {
  const value = String(asset.status || '').toUpperCase()
  if (value === 'MAINTENANCE' || value === 'RETIRED' || value === 'DISPOSED') return 'high'
  if (value === 'IN_USE') return 'medium'
  return 'low'
}

function normalizeBarAssetType(type) {
  const value = String(type || '').toUpperCase()
  const map = {
    EQUIPMENT: '设备资产',
    FACILITY: '站点设施',
    VEHICLE: '车辆资产',
    INVENTORY: '库存资产',
    LICENSE: '数字证书/许可',
    OTHER: '其他资产',
  }
  return map[value] || '其他资产'
}

function mapSiteStatusToAssetStatus(status) {
  return String(status || '').toLowerCase() === 'inactive' ? 'MAINTENANCE' : 'AVAILABLE'
}

function normalizeBarSite(item = {}) {
  const meta = parseBarMeta(item.remark)
  const riskLevel = normalizeRiskLevel(item)
  return {
    id: item.id,
    name: item.name || '',
    url: meta.url || '',
    region: meta.region || '',
    industry: meta.industry || '',
    siteType: meta.siteType || normalizeBarAssetType(item.type),
    loginType: meta.loginType || '',
    remark: meta.remark || '',
    status: normalizeBarStatus(item.status),
    riskLevel,
    hasRisk: riskLevel !== 'low',
    lastVerifyTime: meta.lastVerifyTime || formatDate(item.updatedAt || item.acquireDate),
    accounts: [],
    uks: [],
    attachments: [],
    auditLog: [],
    sop: null,
    assetType: item.type || 'OTHER',
    assetValue: Number(item.value || 0),
    acquireDate: formatDate(item.acquireDate),
    raw: item,
  }
}

function createBarAssetPayload(site = {}) {
  const parsedValue = Number(site.assetValue || 1)
  return {
    name: site.name || '',
    type: site.assetType || 'OTHER',
    value: Number.isFinite(parsedValue) && parsedValue > 0 ? parsedValue : 1,
    status: mapSiteStatusToAssetStatus(site.status),
    acquireDate: site.acquireDate || new Date().toISOString().split('T')[0],
    remark: createBarRemark(site),
  }
}

function filterBarSites(sites, params = {}) {
  return sites.filter((site) => {
    if (params.region && site.region !== params.region) return false
    if (params.industry && site.industry !== params.industry) return false
    if (params.loginType && site.loginType !== params.loginType) return false
    if (params.status && site.status !== params.status) return false
    if (params.riskLevel && site.riskLevel !== params.riskLevel) return false
    return true
  })
}

function normalizeCertificateStatus(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'BORROWED') return 'borrowed'
  if (value === 'EXPIRED') return 'expired'
  if (value === 'DISABLED') return 'disabled'
  return 'available'
}

function normalizeCertificate(item = {}) {
  return {
    id: item.id,
    type: item.type || '',
    provider: item.provider || '',
    serialNo: item.serialNo || '',
    holder: item.holder || '',
    location: item.location || '',
    expiryDate: formatDate(item.expiryDate),
    status: normalizeCertificateStatus(item.status),
    borrower: item.currentBorrower || '',
    borrowProjectId: item.currentProjectId || null,
    borrowPurpose: item.borrowPurpose || '',
    expectedReturn: formatDate(item.expectedReturnDate),
    remark: item.remark || '',
    raw: item,
  }
}

function normalizeApprovalRecord(item = {}) {
  return {
    id: item.id,
    expenseId: item.expenseId,
    projectId: item.projectId || item.raw?.projectId || null,
    project: item.project || (item.projectId ? `项目#${item.projectId}` : `费用#${item.expenseId || '-'}`),
    type: item.type || '',
    amount: Number(item.amount || 0),
    applicant: item.applicant || '',
    applyTime: formatDateTime(item.applyTime || item.createdAt),
    approver: item.approver || '',
    approvalStatus: String(item.result || item.approvalStatus || '').toLowerCase() || 'pending',
    remark: item.comment || item.remark || '',
    raw: item,
  }
}

function filterAccounts(accounts, params = {}) {
  return accounts.filter((account) => {
    if (params.status && account.status !== params.status) return false
    if (params.platform) {
      const keyword = String(params.platform).trim().toLowerCase()
      if (!String(account.platform || '').toLowerCase().includes(keyword)) return false
    }
    return true
  })
}

function filterExpenses(expenses, params = {}) {
  return expenses.filter((item) => {
    if (params.project) {
      const keyword = String(params.project).trim().toLowerCase()
      if (!String(item.project || '').toLowerCase().includes(keyword)) return false
    }
    if (params.type && item.type !== params.type) return false
    if (params.status && item.status !== params.status) return false
    return true
  })
}

function mockDelay(data, total = null, timeout = 200) {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ success: true, data, total: total ?? data.length }), timeout)
  })
}

function getMockAccounts(params = {}) {
  return filterAccounts(mockData.accounts.map(normalizeAccount), params)
}

function getMockBarSites(params = {}) {
  return filterBarSites(mockData.barSites || [], params)
}

function getMockCertificates(siteId) {
  const site = mockData.barSites?.find((item) => String(item.id) === String(siteId))
  return site?.uks || []
}

function getMockExpenses(params = {}) {
  return filterExpenses(mockData.fees.map(normalizeExpense), params)
}

export const accountsApi = {
  async getList(params = {}) {
    if (isMockMode()) {
      const data = filterAccounts(mockData.accounts.map(normalizeAccount), params)
      return mockDelay(data)
    }

    try {
      const response = await httpClient.get('/api/platform/accounts')
      const accounts = Array.isArray(response?.data) ? response.data.map(normalizeAccount) : []
      const filtered = filterAccounts(accounts, params)
      const data = accounts.length > 0 ? filtered : getMockAccounts(params)
      return { success: true, data, total: data.length }
    } catch (error) {
      return {
        success: true,
        data: getMockAccounts(params),
        total: getMockAccounts(params).length,
        message: '使用演示账户数据',
      }
    }
  },

  async getDetail(id) {
    if (isMockMode()) {
      const item = mockData.accounts.find((account) => String(account.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeAccount(item) : null })
    }
    if (!isNumericId(id)) {
      const item = mockData.accounts.find((account) => String(account.id) === String(id))
      return Promise.resolve({ success: Boolean(item), data: item ? normalizeAccount(item) : null })
    }

    try {
      const response = await httpClient.get(`/api/platform/accounts/${id}`)
      return response?.data
        ? { ...response, data: normalizeAccount(response?.data) }
        : { ...response, data: mockData.accounts.find((account) => String(account.id) === String(id)) ? normalizeAccount(mockData.accounts.find((account) => String(account.id) === String(id))) : null }
    } catch (error) {
      const item = mockData.accounts.find((account) => String(account.id) === String(id))
      if (item) {
        return { success: true, data: normalizeAccount(item), message: '使用演示账户数据' }
      }
      throw error
    }
  },

  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeAccount({ ...data, id: `A${Date.now()}` }),
      })
    }
    return httpClient.post('/api/platform/accounts', data)
  },

  async update(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: normalizeAccount({ ...data, id }) })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('account'))

    return httpClient.put(`/api/platform/accounts/${id}`, data)
  },

  async delete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('account'))

    return httpClient.delete(`/api/platform/accounts/${id}`)
  },

  async borrow(id, payload) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeAccount({
          id,
          ...payload,
          status: 'in_use',
          borrower: payload.borrower,
        }),
      })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('account'))

    return httpClient.post(`/api/platform/accounts/${id}/borrow`, payload)
  },

  async return(id) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeAccount({ id, status: 'available', borrower: '' }),
      })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('account'))

    return httpClient.post(`/api/platform/accounts/${id}/return`)
  },

  async getPassword(id) {
    if (isMockMode()) {
      const item = mockData.accounts.find((account) => String(account.id) === String(id))
      return Promise.resolve({ success: true, data: { password: item?.password || '' } })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('account'))

    return httpClient.get(`/api/platform/accounts/${id}/password`)
  },
}

export const barSitesApi = {
  async getList(params = {}) {
    if (isMockMode()) {
      const sites = filterBarSites(mockData.barSites || [], params)
      return mockDelay(sites, null, 300)
    }

    const response = await httpClient.get('/api/resources/bar-assets')
    const page = response?.data
    const content = Array.isArray(page?.content) ? page.content : Array.isArray(response?.data) ? response.data : []
    const sites = filterBarSites(content.map(normalizeBarSite), params)
    const data = content.length > 0 ? sites : getMockBarSites(params)
    return { success: true, data, total: page?.totalElements ?? data.length }
  },

  async getDetail(id) {
    if (isMockMode()) {
      const site = mockData.barSites?.find((item) => String(item.id) === String(id))
      return Promise.resolve({ success: true, data: site || null })
    }
    if (!isNumericId(id)) {
      const site = mockData.barSites?.find((item) => String(item.id) === String(id))
      return Promise.resolve({ success: Boolean(site), data: site || null })
    }

    try {
      const response = await httpClient.get(`/api/resources/bar-assets/${id}`)
      return response?.data
        ? { ...response, data: normalizeBarSite(response?.data) }
        : { ...response, data: null }
    } catch (error) {
      const site = mockData.barSites?.find((item) => String(item.id) === String(id))
      if (site) {
        return { success: true, data: site, message: '使用演示站点数据' }
      }
      throw error
    }
  },

  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          id: `S${Date.now()}`,
          status: 'active',
          hasRisk: false,
          riskLevel: 'low',
          accounts: [],
          uks: [],
          attachments: [],
          auditLog: [],
          sop: null,
          lastVerifyTime: new Date().toISOString().split('T')[0],
          ...data,
        },
      })
    }

    const response = await httpClient.post('/api/resources/bar-assets', createBarAssetPayload(data))
    return { ...response, data: normalizeBarSite(response?.data) }
  },

  async update(id, data) {
    if (isMockMode()) {
      const existing = mockData.barSites?.find((item) => String(item.id) === String(id)) || {}
      return Promise.resolve({
        success: true,
        data: {
          ...existing,
          ...data,
          id,
        },
      })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('bar asset'))

    const response = await httpClient.put(`/api/resources/bar-assets/${id}`, createBarAssetPayload(data))
    return { ...response, data: normalizeBarSite(response?.data) }
  },

  async delete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('bar asset'))

    return httpClient.delete(`/api/resources/bar-assets/${id}`)
  },
}

export const barCertificatesApi = {
  async getList(siteId) {
    if (isMockMode()) {
      const site = mockData.barSites?.find((item) => String(item.id) === String(siteId))
      return Promise.resolve({ success: true, data: site?.uks || [] })
    }
    if (!isNumericId(siteId)) {
      return Promise.resolve({ success: true, data: getMockCertificates(siteId) })
    }

    const response = await httpClient.get(`/api/resources/bar-assets/${siteId}/certificates`)
    const list = Array.isArray(response?.data) ? response.data.map(normalizeCertificate) : []
    return { ...response, data: list.length > 0 ? list : getMockCertificates(siteId) }
  },

  async create(siteId, data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          id: `UK${Date.now()}`,
          status: 'available',
          ...data,
        },
      })
    }
    if (!isNumericId(siteId)) return Promise.resolve(invalidIdMessage('bar asset'))

    const response = await httpClient.post(`/api/resources/bar-assets/${siteId}/certificates`, data)
    return { ...response, data: normalizeCertificate(response?.data) }
  },

  async update(siteId, certificateId, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { ...data, id: certificateId } })
    }
    if (!isNumericId(siteId) || !isNumericId(certificateId)) {
      return Promise.resolve(invalidIdMessage('bar certificate'))
    }

    const response = await httpClient.put(`/api/resources/bar-assets/${siteId}/certificates/${certificateId}`, data)
    return { ...response, data: normalizeCertificate(response?.data) }
  },

  async delete(siteId, certificateId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(siteId) || !isNumericId(certificateId)) {
      return Promise.resolve(invalidIdMessage('bar certificate'))
    }

    return httpClient.delete(`/api/resources/bar-assets/${siteId}/certificates/${certificateId}`)
  },

  async borrow(siteId, certificateId, data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeCertificate({
          id: certificateId,
          ...data,
          currentBorrower: data.borrower,
          currentProjectId: data.projectId,
          borrowPurpose: data.purpose,
          expectedReturnDate: data.expectedReturnDate,
          status: 'BORROWED',
        }),
      })
    }
    if (!isNumericId(siteId) || !isNumericId(certificateId)) {
      return Promise.resolve(invalidIdMessage('bar certificate'))
    }

    const response = await httpClient.post(`/api/resources/bar-assets/${siteId}/certificates/${certificateId}/borrow`, data)
    return { ...response, data: normalizeCertificate(response?.data) }
  },

  async return(siteId, certificateId, data = {}) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeCertificate({ id: certificateId, status: 'AVAILABLE' }),
      })
    }
    if (!isNumericId(siteId) || !isNumericId(certificateId)) {
      return Promise.resolve(invalidIdMessage('bar certificate'))
    }

    const response = await httpClient.post(`/api/resources/bar-assets/${siteId}/certificates/${certificateId}/return`, data)
    return { ...response, data: normalizeCertificate(response?.data) }
  },

  async getBorrowRecords(siteId, certificateId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: [] })
    }
    if (!isNumericId(siteId) || !isNumericId(certificateId)) {
      return Promise.resolve(invalidIdMessage('bar certificate'))
    }

    return httpClient.get(`/api/resources/bar-assets/${siteId}/certificates/${certificateId}/borrow-records`)
  },
}

export const expensesApi = {
  async getList(params = {}) {
    if (isMockMode()) {
      const data = filterExpenses(mockData.fees.map(normalizeExpense), params)
      return mockDelay(data)
    }

    const response = await httpClient.get('/api/resources/expenses')
    const page = response?.data
    const content = Array.isArray(page?.content) ? page.content : Array.isArray(response?.data) ? response.data : []
    const expenses = content.map(normalizeExpense)
    const filtered = filterExpenses(expenses, params)
    const data = content.length > 0 ? filtered : getMockExpenses(params)
    return { success: true, data, total: page?.totalElements ?? data.length }
  },

  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeExpense({ ...data, id: `E${Date.now()}` }),
      })
    }
    const response = await httpClient.post('/api/resources/expenses', data)
    return normalizeExpenseMutationResponse(response)
  },

  async getDetail(id) {
    if (isMockMode()) {
      const item = mockData.fees.find((fee) => String(fee.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeExpense(item) : null })
    }
    if (!isNumericId(id)) {
      const item = mockData.fees.find((fee) => String(fee.id) === String(id))
      return Promise.resolve({ success: Boolean(item), data: item ? normalizeExpense(item) : null })
    }

    try {
      const response = await httpClient.get(`/api/resources/expenses/${id}`)
      return response?.data
        ? { ...response, data: normalizeExpense(response?.data) }
        : { ...response, data: null }
    } catch (error) {
      const item = mockData.fees.find((fee) => String(fee.id) === String(id))
      if (item) {
        return { success: true, data: normalizeExpense(item), message: '使用演示费用数据' }
      }
      throw error
    }
  },

  async getApprovalRecords(projectId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: [] })
    }

    const response = await httpClient.get('/api/resources/expenses/approval-records', {
      params: projectId ? { projectId } : undefined,
    })
    const records = Array.isArray(response?.data) ? response.data.map(normalizeApprovalRecord) : []
    return { ...response, data: records }
  },

  async approve(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('expense'))

    const response = await httpClient.post(`/api/resources/expenses/${id}/approve`, data)
    return normalizeExpenseMutationResponse(response)
  },

  async requestReturn(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('expense'))

    const response = await httpClient.post(`/api/resources/expenses/${id}/return-request`, data)
    return normalizeExpenseMutationResponse(response)
  },

  async confirmReturn(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('expense'))

    const response = await httpClient.post(`/api/resources/expenses/${id}/confirm-return`, data)
    return normalizeExpenseMutationResponse(response)
  },
}

export default {
  accounts: accountsApi,
  barSites: barSitesApi,
  certificates: barCertificatesApi,
  expenses: expensesApi,
}
