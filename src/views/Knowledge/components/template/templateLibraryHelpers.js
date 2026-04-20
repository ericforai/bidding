// Input: template-library page state and normalized template rows
// Output: reusable helpers for filtering, formatting, and dialog form initialization
// Pos: src/views/Knowledge/components/template/ - template page helper layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { getDefaultDocumentTypeForCategory } from '@/config/templateLibrary.js'

export const PROJECT_STATUS_META = {
  pending: { label: '待启动', type: 'info' },
  reviewing: { label: '评审中', type: 'warning' },
  bidding: { label: '投标中', type: 'primary' },
  won: { label: '已中标', type: 'success' },
  lost: { label: '未中标', type: 'danger' }
}

export const USE_TEMPLATE_DOC_TYPE_OPTIONS = [
  { value: 'tech', label: '技术方案', icon: 'Document' },
  { value: 'business', label: '商务应答', icon: 'DocumentCopy' },
  { value: 'contract', label: '合同文档', icon: 'Notebook' },
  { value: 'standalone', label: '独立文档', icon: 'Folder' }
]

export function createTemplateFilters() {
  return {
    name: '',
    productType: '',
    industry: '',
    documentType: '',
    tags: [],
    sort: 'default'
  }
}

export function createUseTemplateForm() {
  return {
    docType: 'standalone',
    projectId: '',
    docName: '',
    applyOptions: ['content', 'format', 'styles']
  }
}

export function createTemplateForm(category = 'technical') {
  return {
    id: null,
    name: '',
    category,
    productType: '',
    industry: '',
    documentType: getDefaultDocumentTypeForCategory(category),
    description: '',
    tagsText: '',
    fileUrl: '',
    fileSize: ''
  }
}

export function patchTemplateForm(target, data = {}) {
  Object.assign(target, createTemplateForm(data.category || 'technical'), {
    id: data.id ?? null,
    name: data.name || '',
    category: data.category || 'technical',
    productType: data.productType || '',
    industry: data.industry || '',
    documentType: data.documentType || getDefaultDocumentTypeForCategory(data.category || 'technical'),
    description: data.description || '',
    tagsText: Array.isArray(data.tags) ? data.tags.join('，') : '',
    fileUrl: data.fileUrl || '',
    fileSize: data.fileSize || ''
  })
}

export function extractTags(tagsText = '') {
  return Array.from(new Set(
    String(tagsText)
      .split(/[，,]/)
      .map((item) => item.trim())
      .filter(Boolean)
  ))
}

export function formatDate(date) {
  if (!date) return '-'
  return String(date).slice(0, 10)
}

export function formatNumber(num) {
  const value = Number(num || 0)
  if (value >= 1000) {
    return `${(value / 1000).toFixed(1)}k`
  }
  return String(value)
}

export function getProjectStatusLabel(status) {
  return PROJECT_STATUS_META[status]?.label || status || '-'
}

export function getProjectStatusType(status) {
  return PROJECT_STATUS_META[status]?.type || 'info'
}

function matchesKeyword(template, keyword) {
  if (!keyword) return true
  const loweredKeyword = String(keyword).toLowerCase()
  return (
    String(template.name || '').toLowerCase().includes(loweredKeyword) ||
    String(template.description || '').toLowerCase().includes(loweredKeyword)
  )
}

function matchesTags(template, tags = []) {
  if (!Array.isArray(tags) || tags.length === 0) return true
  return tags.some((tag) => template.tags.includes(tag))
}

export function sortTemplateCollection(items, sort = 'default') {
  const nextItems = [...items]
  if (sort === 'downloads') {
    return nextItems.sort((left, right) => right.downloads - left.downloads)
  }
  if (sort === 'updateTime') {
    return nextItems.sort((left, right) => new Date(right.updateTime) - new Date(left.updateTime))
  }
  if (sort === 'name') {
    return nextItems.sort((left, right) => String(left.name || '').localeCompare(String(right.name || ''), 'zh'))
  }
  return nextItems
}

export function filterTemplateCollection(items, query = {}) {
  const filtered = items.filter((item) => {
    if (query.category && query.category !== 'all' && item.category !== query.category) {
      return false
    }
    if (query.productType && item.productType !== query.productType) {
      return false
    }
    if (query.industry && item.industry !== query.industry) {
      return false
    }
    if (query.documentType && item.documentType !== query.documentType) {
      return false
    }
    if (!matchesKeyword(item, query.name)) {
      return false
    }
    if (!matchesTags(item, query.tags)) {
      return false
    }
    return true
  })

  return sortTemplateCollection(filtered, query.sort)
}

export function paginateTemplates(items, page, pageSize) {
  const start = (page - 1) * pageSize
  return items.slice(start, start + pageSize)
}

export function buildDocumentDraft(template, useTemplateForm, currentUserName) {
  return {
    id: `doc_${Date.now()}`,
    name: useTemplateForm.docName,
    templateId: template.id,
    templateName: template.name,
    docType: useTemplateForm.docType,
    projectId: useTemplateForm.projectId || null,
    content: template.content || '',
    createdAt: new Date().toISOString(),
    createdBy: currentUserName || '当前用户',
    status: 'draft'
  }
}
