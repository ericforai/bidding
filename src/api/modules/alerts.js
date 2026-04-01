// Input: httpClient, alerts service
// Output: alertRulesApi, alertHistoryApi
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 告警模块 API
 * 支持双模式切换: Mock 数据 / 真实后端 API
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

function normalizeAlertRule(item) {
  return {
    id: item?.id,
    name: item?.name || '未命名规则',
    type: item?.type || 'SYSTEM',
    condition: item?.condition || {},
    enabled: item?.enabled ?? true,
    priority: item?.priority || 'MEDIUM',
    description: item?.description || '',
    actions: item?.actions || [],
    createdAt: item?.createdAt || item?.createTime || '',
    updatedAt: item?.updatedAt || '',
  }
}

function normalizeAlertHistory(item) {
  return {
    id: item?.id,
    ruleId: item?.ruleId || null,
    ruleName: item?.ruleName || '未知规则',
    alertType: item?.alertType || item?.type || 'SYSTEM',
    message: item?.message || item?.alertMessage || '',
    severity: item?.severity || 'INFO',
    status: item?.status || 'ACTIVE',
    projectId: item?.projectId || null,
    projectName: item?.projectName || '',
    createdAt: item?.createdAt || item?.createTime || '',
    acknowledgedAt: item?.acknowledgedAt || null,
    resolvedAt: item?.resolvedAt || null,
  }
}

export const alertRulesApi = {
  async getList() {
    if (isMockMode()) {
      const data = (mockData.alertRules || []).map(normalizeAlertRule)
      return Promise.resolve({ success: true, data })
    }
    const response = await httpClient.get('/api/alerts/rules')
    return { ...response, data: (response?.data || []).map(normalizeAlertRule) }
  },

  async getDetail(id) {
    if (isMockMode()) {
      const item = (mockData.alertRules || []).find((r) => String(r.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeAlertRule(item) : null })
    }
    const response = await httpClient.get(`/api/alerts/rules/${id}`)
    return { ...response, data: normalizeAlertRule(response?.data) }
  },

  async getEnabled() {
    if (isMockMode()) {
      const data = (mockData.alertRules || []).filter((r) => r.enabled).map(normalizeAlertRule)
      return Promise.resolve({ success: true, data })
    }
    const response = await httpClient.get('/api/alerts/rules/enabled')
    return { ...response, data: (response?.data || []).map(normalizeAlertRule) }
  },

  async create(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeAlertRule({ ...data, id: `AR${Date.now()}`, enabled: true }),
      })
    }
    const response = await httpClient.post('/api/alerts/rules', data)
    return { ...response, data: normalizeAlertRule(response?.data) }
  },

  async update(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: normalizeAlertRule({ ...data, id }) })
    }
    const response = await httpClient.put(`/api/alerts/rules/${id}`, data)
    return { ...response, data: normalizeAlertRule({ ...response?.data, ...data }) }
  },

  async delete(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    return httpClient.delete(`/api/alerts/rules/${id}`)
  },

  async toggle(id) {
    if (isMockMode()) {
      const item = (mockData.alertRules || []).find((r) => String(r.id) === String(id))
      return Promise.resolve({ success: true, data: normalizeAlertRule({ ...item, enabled: !item?.enabled }) })
    }
    const response = await httpClient.patch(`/api/alerts/rules/${id}/toggle`)
    return { ...response, data: normalizeAlertRule(response?.data) }
  },
}

export const alertHistoryApi = {
  async getList(params = {}) {
    if (isMockMode()) {
      const data = (mockData.alertHistory || []).map(normalizeAlertHistory)
      return Promise.resolve({ success: true, data, total: data.length })
    }
    const response = await httpClient.get('/api/alerts/history', { params })
    return {
      ...response,
      data: response?.data?.content ? response.data.content.map(normalizeAlertHistory) : (response?.data || []).map(normalizeAlertHistory),
      total: response?.data?.totalElements || response?.data?.length || 0,
    }
  },

  async getDetail(id) {
    if (isMockMode()) {
      const item = (mockData.alertHistory || []).find((h) => String(h.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeAlertHistory(item) : null })
    }
    const response = await httpClient.get(`/api/alerts/history/${id}`)
    return { ...response, data: normalizeAlertHistory(response?.data) }
  },

  async acknowledge(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    return httpClient.patch(`/api/alerts/history/${id}/acknowledge`)
  },
}

export default {
  alertRules: alertRulesApi,
  alertHistory: alertHistoryApi,
}
