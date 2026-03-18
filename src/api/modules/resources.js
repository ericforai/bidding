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

function normalizeBarSiteAccount(item = {}) {
  return {
    id: item.id,
    username: item.username || '',
    role: item.role || 'viewer',
    owner: item.owner || '',
    phone: item.phone || '',
    email: item.email || '',
    status: item.status || 'active',
    raw: item,
  }
}

function normalizeBarSiteAttachment(item = {}) {
  return {
    id: item.id,
    name: item.name || '',
    size: item.size || '',
    contentType: item.contentType || '',
    url: item.url || '',
    uploadedBy: item.uploadedBy || '',
    uploadedAt: formatDateTime(item.uploadedAt),
    raw: item,
  }
}

function normalizeBarVerification(item = {}) {
  return {
    id: item.id,
    verifiedBy: item.verifiedBy || '',
    verifiedAt: formatDateTime(item.verifiedAt),
    status: String(item.status || '').toUpperCase() || 'SUCCESS',
    message: item.message || '',
    raw: item,
  }
}

function normalizeBarSop(item = {}) {
  return {
    resetUrl: item.resetUrl || '',
    unlockUrl: item.unlockUrl || '',
    contacts: Array.isArray(item.contacts) ? item.contacts : [],
    requiredDocs: Array.isArray(item.requiredDocs) ? item.requiredDocs : [],
    faqs: Array.isArray(item.faqs) ? item.faqs : [],
    history: Array.isArray(item.history) ? item.history : [],
    estimatedTime: item.estimatedTime || '',
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

export const accountsApi = {
  async getList(params = {}) {
    if (isMockMode()) {
      const data = filterAccounts(mockData.accounts.map(normalizeAccount), params)
      return mockDelay(data)
    }

    const response = await httpClient.get('/api/platform/accounts')
    const page = response?.data
    const content = Array.isArray(page?.content) ? page.content : Array.isArray(response?.data) ? response.data : []
    const accounts = content.map(normalizeAccount)
    const data = filterAccounts(accounts, params)
    return { ...response, data, total: page?.totalElements ?? data.length }
  },

  async getDetail(id) {
    if (isMockMode()) {
      const item = mockData.accounts.find((account) => String(account.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeAccount(item) : null })
    }
    if (!isNumericId(id)) {
      return Promise.resolve(invalidIdMessage('account'))
    }

    const response = await httpClient.get(`/api/platform/accounts/${id}`)
    return response?.data ? { ...response, data: normalizeAccount(response?.data) } : { ...response, data: null }
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
    return { ...response, data: sites, total: page?.totalElements ?? sites.length }
  },

  async getDetail(id) {
    if (isMockMode()) {
      const site = mockData.barSites?.find((item) => String(item.id) === String(id))
      return Promise.resolve({ success: true, data: site || null })
    }
    if (!isNumericId(id)) {
      return Promise.resolve(invalidIdMessage('bar asset'))
    }

    const response = await httpClient.get(`/api/resources/bar-assets/${id}`)
    return response?.data ? { ...response, data: normalizeBarSite(response?.data) } : { ...response, data: null }
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

  async updateStatus(id, status) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: { id, status: status === 'inactive' ? 'inactive' : 'active' },
      })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('bar asset'))

    const response = await httpClient.patch(`/api/resources/bar-assets/${id}/status`, { status })
    return { ...response, data: normalizeBarSite(response?.data) }
  },

  async verify(id, payload = {}) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeBarVerification({
          id: `VERIFY-${Date.now()}`,
          verifiedBy: payload.verifiedBy || 'system',
          verifiedAt: new Date().toISOString(),
          status: payload.status || 'SUCCESS',
          message: payload.message || '站点连通性校验通过',
        }),
      })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('bar asset'))

    const response = await httpClient.post(`/api/resources/bar-assets/${id}/verify`, payload)
    return { ...response, data: normalizeBarVerification(response?.data) }
  },

  async getVerificationRecords(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: [] })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('bar asset'))

    const response = await httpClient.get(`/api/resources/bar-assets/${id}/verification-records`)
    return {
      ...response,
      data: Array.isArray(response?.data) ? response.data.map(normalizeBarVerification) : [],
    }
  },

  async delete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('bar asset'))

    return httpClient.delete(`/api/resources/bar-assets/${id}`)
  },
}

export const barSiteAccountsApi = {
  async getList(siteId) {
    if (isMockMode()) {
      const site = mockData.barSites?.find((item) => String(item.id) === String(siteId))
      return Promise.resolve({ success: true, data: site?.accounts || [] })
    }
    if (!isNumericId(siteId)) {
      return Promise.resolve(invalidIdMessage('bar site account'))
    }

    const response = await httpClient.get(`/api/resources/bar-assets/${siteId}/accounts`)
    const page = response?.data
    const content = Array.isArray(page?.content) ? page.content : Array.isArray(response?.data) ? response.data : []
    return {
      ...response,
      data: content.map(normalizeBarSiteAccount),
    }
  },

  async create(siteId, data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: { id: `A${Date.now()}`, status: 'active', ...data },
      })
    }
    if (!isNumericId(siteId)) return Promise.resolve(invalidIdMessage('bar asset'))

    const response = await httpClient.post(`/api/resources/bar-assets/${siteId}/accounts`, data)
    return { ...response, data: normalizeBarSiteAccount(response?.data) }
  },

  async update(siteId, accountId, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { ...data, id: accountId } })
    }
    if (!isNumericId(siteId) || !isNumericId(accountId)) return Promise.resolve(invalidIdMessage('bar site account'))

    const response = await httpClient.put(`/api/resources/bar-assets/${siteId}/accounts/${accountId}`, data)
    return { ...response, data: normalizeBarSiteAccount(response?.data) }
  },

  async delete(siteId, accountId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(siteId) || !isNumericId(accountId)) return Promise.resolve(invalidIdMessage('bar site account'))

    return httpClient.delete(`/api/resources/bar-assets/${siteId}/accounts/${accountId}`)
  },
}

export const barSiteSopApi = {
  async get(siteId) {
    if (isMockMode()) {
      const site = mockData.barSites?.find((item) => String(item.id) === String(siteId))
      return Promise.resolve({ success: true, data: site?.sop || null })
    }
    if (!isNumericId(siteId)) {
      return Promise.resolve(invalidIdMessage('bar asset'))
    }

    const response = await httpClient.get(`/api/resources/bar-assets/${siteId}/sop`)
    return { ...response, data: normalizeBarSop(response?.data || {}) }
  },

  async update(siteId, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data })
    }
    if (!isNumericId(siteId)) return Promise.resolve(invalidIdMessage('bar asset'))

    const response = await httpClient.put(`/api/resources/bar-assets/${siteId}/sop`, data)
    return { ...response, data: normalizeBarSop(response?.data || {}) }
  },
}

export const barSiteAttachmentsApi = {
  async getList(siteId) {
    if (isMockMode()) {
      const site = mockData.barSites?.find((item) => String(item.id) === String(siteId))
      return Promise.resolve({ success: true, data: site?.attachments || [] })
    }
    if (!isNumericId(siteId)) {
      return Promise.resolve(invalidIdMessage('bar site attachment'))
    }

    const response = await httpClient.get(`/api/resources/bar-assets/${siteId}/attachments`)
    const page = response?.data
    const content = Array.isArray(page?.content) ? page.content : Array.isArray(response?.data) ? response.data : []
    return {
      ...response,
      data: content.map(normalizeBarSiteAttachment),
    }
  },

  async create(siteId, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: { id: `ATT-${Date.now()}`, ...data } })
    }
    if (!isNumericId(siteId)) return Promise.resolve(invalidIdMessage('bar asset'))

    const response = await httpClient.post(`/api/resources/bar-assets/${siteId}/attachments`, data)
    return { ...response, data: normalizeBarSiteAttachment(response?.data) }
  },

  async delete(siteId, attachmentId) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(siteId) || !isNumericId(attachmentId)) return Promise.resolve(invalidIdMessage('bar site attachment'))

    return httpClient.delete(`/api/resources/bar-assets/${siteId}/attachments/${attachmentId}`)
  },
}

export const barCertificatesApi = {
  async getList(siteId) {
    if (isMockMode()) {
      const site = mockData.barSites?.find((item) => String(item.id) === String(siteId))
      return Promise.resolve({ success: true, data: site?.uks || [] })
    }
    if (!isNumericId(siteId)) {
      return Promise.resolve(invalidIdMessage('bar certificate'))
    }

    const response = await httpClient.get(`/api/resources/bar-assets/${siteId}/certificates`)
    const page = response?.data
    const content = Array.isArray(page?.content) ? page.content : Array.isArray(response?.data) ? response.data : []
    const list = content.map(normalizeCertificate)
    return { ...response, data: list }
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
    const data = filterExpenses(expenses, params)
    return { ...response, data, total: page?.totalElements ?? data.length }
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
      return Promise.resolve(invalidIdMessage('expense'))
    }

    const response = await httpClient.get(`/api/resources/expenses/${id}`)
    return response?.data ? { ...response, data: normalizeExpense(response?.data) } : { ...response, data: null }
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
  barSiteAccounts: barSiteAccountsApi,
  barSiteSop: barSiteSopApi,
  barSiteAttachments: barSiteAttachmentsApi,
  certificates: barCertificatesApi,
  expenses: expensesApi,
}
