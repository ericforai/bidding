// Input: httpClient, API mode config, knowledge normalizers and demo adapters
// Output: knowledgeApi - qualification, case, and template accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 知识库模块 API (资质、案例、模板)
 * 真实 API 知识库访问层
 */
import httpClient from '../client.js'
import { qualificationsApi } from './qualification.js'

const DAY_IN_MS = 24 * 60 * 60 * 1000

const qualificationTypeMap = {
  enterprise: 'CONSTRUCTION',
  personnel: 'DESIGN',
  product: 'SERVICE',
  industry: 'OTHER',
  '企业资质': 'enterprise',
  '软件能力': 'product',
  '安全资质': 'industry',
  CONSTRUCTION: 'enterprise',
  DESIGN: 'personnel',
  SERVICE: 'product',
  OTHER: 'industry' }

const qualificationLevelMap = {
  enterprise: 'FIRST',
  personnel: 'SECOND',
  product: 'THIRD',
  industry: 'OTHER' }

const caseIndustryMap = {
  government: 'INFRASTRUCTURE',
  finance: 'OTHER',
  energy: 'ENERGY',
  transport: 'TRANSPORTATION',
  healthcare: 'OTHER',
  education: 'OTHER',
  manufacturing: 'MANUFACTURING',
  internet: 'OTHER',
  政府: 'government',
  能源: 'energy',
  交通: 'transport',
  制造业: 'manufacturing',
  教育: 'education',
  医疗: 'healthcare',
  互联网: 'internet',
  园区: 'government',
  INFRASTRUCTURE: 'government',
  MANUFACTURING: 'manufacturing',
  ENERGY: 'energy',
  TRANSPORTATION: 'transport',
  ENVIRONMENTAL: 'government',
  REAL_ESTATE: 'government',
  OTHER: 'government' }

const templateCategoryMap = {
  technical: 'TECHNICAL',
  commercial: 'COMMERCIAL',
  implementation: 'OTHER',
  quotation: 'LEGAL',
  qualification: 'QUALIFICATION',
  contract: 'CONTRACT',
  技术方案: 'technical',
  商务文件: 'commercial',
  行业方案: 'implementation',
  实施方案: 'implementation',
  资质文件: 'qualification',
  合同范本: 'contract',
  TECHNICAL: 'technical',
  COMMERCIAL: 'commercial',
  LEGAL: 'quotation',
  QUALIFICATION: 'qualification',
  CONTRACT: 'contract',
  OTHER: 'implementation' }

function isNumericId(id) {
  return /^\d+$/.test(String(id))
}

function getDateValue(date) {
  return date ? new Date(date) : null
}

function calculateRemainingDays(expiryDate) {
  const expiry = getDateValue(expiryDate)
  if (!expiry) return null
  return Math.ceil((expiry.getTime() - Date.now()) / DAY_IN_MS)
}

function mapQualificationStatus(expiryDate, explicitStatus) {
  if (explicitStatus) {
    return explicitStatus
  }

  const remainingDays = calculateRemainingDays(expiryDate)
  if (remainingDays == null) return 'valid'
  if (remainingDays < 0) return 'expired'
  if (remainingDays <= 30) return 'expiring'
  return 'valid'
}

function formatDate(date) {
  if (!date) return ''
  return String(date).slice(0, 10)
}

function formatCasePeriod(projectDate) {
  const date = formatDate(projectDate)
  return date ? `${date} - ${date}` : '-'
}

function normalizeQualification(item) {
  const expiryDate = item?.expiryDate || item?.expiry
  const remainingDays = item?.remainingDays ?? calculateRemainingDays(expiryDate)

  return {
    id: item?.id,
    name: item?.name || '未命名资质',
    type: qualificationTypeMap[item?.type] || 'industry',
    certificateNo: item?.certificateNo || '-',
    issueDate: formatDate(item?.issueDate),
    expiryDate: formatDate(expiryDate),
    issuer: item?.issuer || '-',
    status: mapQualificationStatus(expiryDate, item?.status),
    remainingDays: remainingDays ?? 0,
    fileUrl: item?.fileUrl || '',
    level: item?.level || qualificationLevelMap[qualificationTypeMap[item?.type] || item?.type] || 'OTHER' }
}

function buildQualificationPayload(data = {}) {
  return {
    name: data.name,
    type: qualificationTypeMap[data.type] || 'OTHER',
    level: qualificationLevelMap[data.type] || data.level || 'OTHER',
    issueDate: data.issueDate || null,
    expiryDate: data.expiryDate || null,
    fileUrl: data.fileUrl || '' }
}

function normalizeCase(item) {
  const projectDate = formatDate(item?.projectDate)
  const normalizedIndustry = caseIndustryMap[item?.industry] || 'government'
  const description = item?.description || item?.summary || '暂无描述'
  const customer = item?.customer || item?.customerName || '待补充'
  const location = item?.location || item?.locationName || '-'
  const period = item?.period || item?.projectPeriod || formatCasePeriod(projectDate)
  const technologies = Array.isArray(item?.technologies) ? item.technologies : []
  const archivedInfo = item?.archivedInfo || {
    techHighlights: item?.techHighlights || '',
    priceStrategy: item?.priceStrategy || '',
    successFactors: item?.successFactors || [],
    lessons: item?.lessons || '',
    attachments: item?.attachments || [] }

  return {
    id: item?.id,
    title: item?.title || '未命名案例',
    customer,
    customerName: customer,
    industry: normalizedIndustry,
    amount: Number(item?.amount || 0),
    year: item?.year || (projectDate ? new Date(projectDate).getFullYear() : new Date().getFullYear()),
    location,
    locationName: location,
    period,
    projectPeriod: period,
    tags: Array.isArray(item?.tags) ? item.tags : [],
    highlights: Array.isArray(item?.highlights) ? item.highlights : [],
    description,
    summary: item?.summary || description,
    technologies,
    viewCount: Number(item?.viewCount || 0),
    useCount: Number(item?.useCount || 0),
    archivedInfo }
}

function buildCasePayload(data = {}) {
  const projectDate = Array.isArray(data.period) && data.period.length
    ? data.period[data.period.length - 1]
    : data.projectDate || new Date().toISOString().slice(0, 10)

  return {
    title: data.title,
    industry: caseIndustryMap[data.industry] || 'OTHER',
    outcome: data.outcome || 'WON',
    amount: data.amount ?? 0,
    projectDate,
    description: data.description || data.summary || '',
    customerName: data.customerName || data.customer || '',
    locationName: data.locationName || data.location || '',
    projectPeriod: data.projectPeriod || data.period || '',
    tags: Array.isArray(data.tags) ? data.tags : [],
    highlights: Array.isArray(data.highlights) ? data.highlights : [],
    technologies: Array.isArray(data.technologies) ? data.technologies : [],
    viewCount: Number(data.viewCount || 0),
    useCount: Number(data.useCount || 0) }
}

function normalizeTemplate(item) {
  const category = templateCategoryMap[item?.category] || 'implementation'
  const updateTime = formatDate(item?.updatedAt || item?.createdAt || item?.updateTime)

  return {
    id: item?.id,
    name: item?.name || '未命名模板',
    category,
    tags: Array.isArray(item?.tags) ? item.tags : [],
    description: item?.description || '暂无真实模板描述',
    downloads: Number(item?.downloads || 0),
    useCount: Number(item?.useCount || 0),
    updateTime: updateTime || '-',
    version: item?.currentVersion || item?.version || '1.0',
    fileSize: item?.fileSize || '未知',
    fileUrl: item?.fileUrl || '',
    content: item?.content || '',
    structure: Array.isArray(item?.structure) ? item.structure : [],
    createdBy: item?.createdBy || null }
}

function buildTemplatePayload(data = {}) {
  return {
    name: data.name,
    category: templateCategoryMap[data.category] || 'OTHER',
    fileUrl: data.fileUrl || '',
    description: data.description || '',
    fileSize: data.fileSize || '',
    tags: Array.isArray(data.tags) ? data.tags : [],
    createdBy: data.createdBy ?? null }
}

function filterQualifications(items, params = {}) {
  return items.filter((item) => {
    if (params.name && !String(item.name || '').toLowerCase().includes(String(params.name).toLowerCase())) {
      return false
    }
    if (params.type && item.type !== params.type) {
      return false
    }
    if (params.status && item.status !== params.status) {
      return false
    }
    return true
  })
}

function filterCases(items, params = {}) {
  return items.filter((item) => {
    if (params.industry && item.industry !== params.industry) {
      return false
    }
    if (params.keyword) {
      const keyword = String(params.keyword).toLowerCase()
      const matchesKeyword =
        String(item.title || '').toLowerCase().includes(keyword) ||
        String(item.customer || '').toLowerCase().includes(keyword) ||
        item.highlights.some((highlight) => String(highlight).toLowerCase().includes(keyword))
      if (!matchesKeyword) {
        return false
      }
    }
    return true
  })
}

function filterTemplates(items, params = {}) {
  return items.filter((item) => {
    if (params.category && params.category !== 'all' && item.category !== params.category) {
      return false
    }
    if (params.name) {
      const keyword = String(params.name).toLowerCase()
      const matchesKeyword =
        String(item.name || '').toLowerCase().includes(keyword) ||
        String(item.description || '').toLowerCase().includes(keyword)
      if (!matchesKeyword) {
        return false
      }
    }
    if (Array.isArray(params.tags) && params.tags.length > 0) {
      const matchesTags = params.tags.some((tag) => item.tags.includes(tag))
      if (!matchesTags) {
        return false
      }
    }
    return true
  })
}

async function fetchAndFilter(path, params, normalizer, filterFn) {
  const response = await httpClient.get(path)
  const normalized = Array.isArray(response?.data) ? response.data.map(normalizer) : []
  const filtered = filterFn(normalized, params)
  const data = filtered

  return {
    ...response,
    data,
    total: data.length }
}

function invalidIdMessage(entityName) {
  return {
    success: false,
    message: `Current backend only supports numeric ${entityName} IDs in API mode` }
}

export const casesApi = {
  async getList(params) {

    return fetchAndFilter('/api/knowledge/cases', params, normalizeCase, filterCases)
  },

  async getDetail(id) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('case'))

    const response = await httpClient.get(`/api/knowledge/cases/${id}`)
    return { ...response, data: normalizeCase(response?.data) }
  },

  async create(data) {

    const response = await httpClient.post('/api/knowledge/cases', buildCasePayload(data))
    return { ...response, data: normalizeCase({ ...response?.data, ...data, viewCount: 0, useCount: 0 }) }
  },

  async update(id, data) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('case'))

    const response = await httpClient.put(`/api/knowledge/cases/${id}`, buildCasePayload(data))
    return { ...response, data: normalizeCase({ ...response?.data, ...data, id }) }
  },

  async delete(id) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('case'))
    return httpClient.delete(`/api/knowledge/cases/${id}`)
  },

  async getShareRecords(id) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('case'))
    return httpClient.get(`/api/knowledge/cases/${id}/share-records`)
  },

  async createShareRecord(id, data = {}) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('case'))
    return httpClient.post(`/api/knowledge/cases/${id}/share-records`, {
      createdBy: data.createdBy ?? null,
      createdByName: data.createdByName || '',
      baseUrl: data.baseUrl || window.location.origin,
      expiresAt: data.expiresAt ?? null })
  },

  async getReferenceRecords(id) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('case'))
    return httpClient.get(`/api/knowledge/cases/${id}/references`)
  },

  async createReferenceRecord(id, data = {}) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('case'))
    return httpClient.post(`/api/knowledge/cases/${id}/references`, {
      referencedBy: data.referencedBy ?? null,
      referencedByName: data.referencedByName || '',
      referenceTarget: data.referenceTarget || '',
      referenceContext: data.referenceContext || '' })
  } }

export const templatesApi = {
  async getList(params) {

    return fetchAndFilter('/api/knowledge/templates', params, normalizeTemplate, filterTemplates)
  },

  async getDetail(id) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('template'))

    const response = await httpClient.get(`/api/knowledge/templates/${id}`)
    return { ...response, data: normalizeTemplate(response?.data) }
  },

  async create(data) {

    const response = await httpClient.post('/api/knowledge/templates', buildTemplatePayload(data))
    return { ...response, data: normalizeTemplate({ ...response?.data, ...data }) }
  },

  async update(id, data) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('template'))

    const response = await httpClient.put(`/api/knowledge/templates/${id}`, buildTemplatePayload(data))
    return { ...response, data: normalizeTemplate({ ...response?.data, ...data, id }) }
  },

  async delete(id) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('template'))
    return httpClient.delete(`/api/knowledge/templates/${id}`)
  },

  async copy(id, data = {}) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('template'))

    const response = await httpClient.post(`/api/knowledge/templates/${id}/copy`, {
      name: data.name,
      createdBy: data.createdBy ?? null })
    return { ...response, data: normalizeTemplate(response?.data) }
  },

  async getVersions(id) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('template'))

    return httpClient.get(`/api/knowledge/templates/${id}/versions`)
  },

  async recordUse(id, data = {}) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('template'))

    return httpClient.post(`/api/knowledge/templates/${id}/use-records`, {
      documentName: data.documentName,
      docType: data.docType,
      projectId: data.projectId ?? null,
      applyOptions: Array.isArray(data.applyOptions) ? data.applyOptions : [],
      usedBy: data.usedBy ?? null })
  },

  async recordDownload(id, data = {}) {
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('template'))

    const response = await httpClient.post(`/api/knowledge/templates/${id}/downloads`, {
      downloadedBy: data.downloadedBy ?? null })
    return { ...response, data: normalizeTemplate(response?.data) }
  } }

export default {
  qualifications: qualificationsApi,
  cases: casesApi,
  templates: templatesApi }
