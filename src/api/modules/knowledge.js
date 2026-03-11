/**
 * 知识库模块 API (资质、案例、模板)
 * 支持双模式切换，并在 API 模式下对齐现有后端契约
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

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
  OTHER: 'industry',
}

const qualificationLevelMap = {
  enterprise: 'FIRST',
  personnel: 'SECOND',
  product: 'THIRD',
  industry: 'OTHER',
}

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
  OTHER: 'government',
}

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
  OTHER: 'implementation',
}

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
    level: item?.level || qualificationLevelMap[qualificationTypeMap[item?.type] || item?.type] || 'OTHER',
  }
}

function buildQualificationPayload(data = {}) {
  return {
    name: data.name,
    type: qualificationTypeMap[data.type] || 'OTHER',
    level: qualificationLevelMap[data.type] || data.level || 'OTHER',
    issueDate: data.issueDate || null,
    expiryDate: data.expiryDate || null,
    fileUrl: data.fileUrl || '',
  }
}

function normalizeCase(item) {
  const projectDate = formatDate(item?.projectDate)
  const normalizedIndustry = caseIndustryMap[item?.industry] || 'government'

  return {
    id: item?.id,
    title: item?.title || '未命名案例',
    customer: item?.customer || '待补充',
    industry: normalizedIndustry,
    amount: Number(item?.amount || 0),
    year: item?.year || (projectDate ? new Date(projectDate).getFullYear() : new Date().getFullYear()),
    location: item?.location || '-',
    period: item?.period || formatCasePeriod(projectDate),
    tags: Array.isArray(item?.tags) ? item.tags : [],
    highlights: Array.isArray(item?.highlights) ? item.highlights : [],
    description: item?.description || item?.summary || '暂无描述',
    viewCount: Number(item?.viewCount || 0),
    useCount: Number(item?.useCount || 0),
    archivedInfo: item?.archivedInfo || {
      techHighlights: item?.techHighlights || '',
      priceStrategy: item?.priceStrategy || '',
      successFactors: item?.successFactors || [],
      lessons: item?.lessons || '',
      attachments: item?.attachments || [],
    },
  }
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
    description: data.description || '',
  }
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
    updateTime: updateTime || '-',
    version: item?.version || '1.0',
    fileSize: item?.fileSize || '未知',
    fileUrl: item?.fileUrl || '',
    content: item?.content || '',
    structure: Array.isArray(item?.structure) ? item.structure : [],
    createdBy: item?.createdBy || null,
  }
}

function buildTemplatePayload(data = {}) {
  return {
    name: data.name,
    category: templateCategoryMap[data.category] || 'OTHER',
    fileUrl: data.fileUrl || '',
    tags: Array.isArray(data.tags) ? data.tags : [],
    createdBy: data.createdBy ?? null,
  }
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

function getMockQualifications(params = {}) {
  return filterQualifications(mockData.qualifications.map(normalizeQualification), params)
}

function getMockCases(params = {}) {
  return filterCases(mockData.cases.map(normalizeCase), params)
}

function getMockTemplates(params = {}) {
  return filterTemplates(mockData.templates.map(normalizeTemplate), params)
}

async function fetchAndFilter(path, params, normalizer, filterFn, fallbackFactory) {
  const response = await httpClient.get(path)
  const normalized = Array.isArray(response?.data) ? response.data.map(normalizer) : []
  const filtered = filterFn(normalized, params)
  const data = normalized.length > 0 ? filtered : fallbackFactory(params)

  return {
    ...response,
    data,
    total: data.length,
  }
}

function invalidIdMessage(entityName) {
  return {
    success: false,
    message: `Current backend only supports numeric ${entityName} IDs in API mode`,
  }
}

export const qualificationsApi = {
  async getList(params) {
    if (isMockMode()) {
      const data = filterQualifications(mockData.qualifications.map(normalizeQualification), params)
      return Promise.resolve({ success: true, data, total: data.length })
    }

    return fetchAndFilter('/api/knowledge/qualifications', params, normalizeQualification, filterQualifications, getMockQualifications)
  },

  async getDetail(id) {
    if (isMockMode()) {
      const item = mockData.qualifications.find((qualification) => String(qualification.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeQualification(item) : null })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('qualification'))

    const response = await httpClient.get(`/api/knowledge/qualifications/${id}`)
    return { ...response, data: normalizeQualification(response?.data) }
  },

  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeQualification({
          ...data,
          id: `Q${Date.now()}`,
          status: 'valid',
        }),
      })
    }

    const response = await httpClient.post('/api/knowledge/qualifications', buildQualificationPayload(data))
    return { ...response, data: normalizeQualification({ ...response?.data, ...data }) }
  },

  async update(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: normalizeQualification({ ...data, id }) })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('qualification'))

    const response = await httpClient.put(`/api/knowledge/qualifications/${id}`, buildQualificationPayload(data))
    return { ...response, data: normalizeQualification({ ...response?.data, ...data, id }) }
  },

  async delete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('qualification'))
    return httpClient.delete(`/api/knowledge/qualifications/${id}`)
  },
}

export const casesApi = {
  async getList(params) {
    if (isMockMode()) {
      const data = filterCases(mockData.cases.map(normalizeCase), params)
      return Promise.resolve({ success: true, data, total: data.length })
    }

    return fetchAndFilter('/api/knowledge/cases', params, normalizeCase, filterCases, getMockCases)
  },

  async getDetail(id) {
    if (isMockMode()) {
      const item = mockData.cases.find((caseItem) => String(caseItem.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeCase(item) : null })
    }
    if (!isNumericId(id)) {
      const item = mockData.cases.find((caseItem) => String(caseItem.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeCase(item) : null })
    }

    const response = await httpClient.get(`/api/knowledge/cases/${id}`)
    return { ...response, data: normalizeCase(response?.data) }
  },

  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeCase({
          ...data,
          id: `C${Date.now()}`,
          year: new Date().getFullYear(),
          viewCount: 0,
          useCount: 0,
        }),
      })
    }

    const response = await httpClient.post('/api/knowledge/cases', buildCasePayload(data))
    return { ...response, data: normalizeCase({ ...response?.data, ...data, viewCount: 0, useCount: 0 }) }
  },

  async update(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: normalizeCase({ ...data, id }) })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('case'))

    const response = await httpClient.put(`/api/knowledge/cases/${id}`, buildCasePayload(data))
    return { ...response, data: normalizeCase({ ...response?.data, ...data, id }) }
  },

  async delete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('case'))
    return httpClient.delete(`/api/knowledge/cases/${id}`)
  },
}

export const templatesApi = {
  async getList(params) {
    if (isMockMode()) {
      const data = filterTemplates(mockData.templates.map(normalizeTemplate), params)
      return Promise.resolve({ success: true, data, total: data.length })
    }

    return fetchAndFilter('/api/knowledge/templates', params, normalizeTemplate, filterTemplates, getMockTemplates)
  },

  async getDetail(id) {
    if (isMockMode()) {
      const item = mockData.templates.find((template) => String(template.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeTemplate(item) : null })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('template'))

    const response = await httpClient.get(`/api/knowledge/templates/${id}`)
    return { ...response, data: normalizeTemplate(response?.data) }
  },

  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeTemplate({
          ...data,
          id: `TP${Date.now()}`,
          downloads: 0,
          version: '1.0',
          updateTime: new Date().toISOString().slice(0, 10),
        }),
      })
    }

    const response = await httpClient.post('/api/knowledge/templates', buildTemplatePayload(data))
    return { ...response, data: normalizeTemplate({ ...response?.data, ...data }) }
  },

  async update(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: normalizeTemplate({ ...data, id }) })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('template'))

    const response = await httpClient.put(`/api/knowledge/templates/${id}`, buildTemplatePayload(data))
    return { ...response, data: normalizeTemplate({ ...response?.data, ...data, id }) }
  },

  async delete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    if (!isNumericId(id)) return Promise.resolve(invalidIdMessage('template'))
    return httpClient.delete(`/api/knowledge/templates/${id}`)
  },
}

export default {
  qualifications: qualificationsApi,
  cases: casesApi,
  templates: templatesApi,
}
