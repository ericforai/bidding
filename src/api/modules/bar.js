// Input: httpClient, bar asset service
// Output: barAssetsApi
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * BAR资产管理模块 API
 * 支持双模式切换: Mock 数据 / 真实后端 API
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

function normalizeAsset(item) {
  return {
    id: item?.id,
    name: item?.name || '未命名资产',
    category: item?.category || 'OTHER',
    serialNumber: item?.serialNumber || '',
    purchaseDate: item?.purchaseDate || '',
    status: item?.status || 'ACTIVE',
    owner: item?.owner || '',
    location: item?.location || '',
    value: item?.value || 0,
    description: item?.description || '',
    createdAt: item?.createdAt || '',
    updatedAt: item?.updatedAt || '',
  }
}

function normalizeCertificate(item) {
  return {
    id: item?.id,
    assetId: item?.assetId || null,
    assetName: item?.assetName || '',
    certificateType: item?.certificateType || 'LICENSE',
    certificateNumber: item?.certificateNumber || '',
    issueDate: item?.issueDate || '',
    expiryDate: item?.expiryDate || '',
    issuingAuthority: item?.issuingAuthority || '',
    status: item?.status || 'VALID',
    createdAt: item?.createdAt || '',
    updatedAt: item?.updatedAt || '',
  }
}

export const barAssetsApi = {
  async getAssets(params = {}) {
    if (isMockMode()) {
      const data = (mockData.barAssets || []).map(normalizeAsset)
      return Promise.resolve({ success: true, data, total: data.length })
    }
    const response = await httpClient.get('/api/resources/bar-assets', { params })
    return {
      ...response,
      data: response?.data?.content ? response.data.content.map(normalizeAsset) : (response?.data || []).map(normalizeAsset),
      total: response?.data?.totalElements || response?.data?.length || 0,
    }
  },

  async getAsset(id) {
    if (isMockMode()) {
      const item = (mockData.barAssets || []).find((a) => String(a.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeAsset(item) : null })
    }
    const response = await httpClient.get(`/api/resources/bar-assets/${id}`)
    return { ...response, data: normalizeAsset(response?.data) }
  },

  async createAsset(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeAsset({ ...data, id: `BA${Date.now()}` }),
      })
    }
    const response = await httpClient.post('/api/resources/bar-assets', data)
    return { ...response, data: normalizeAsset(response?.data) }
  },

  async updateAsset(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: normalizeAsset({ ...data, id }) })
    }
    const response = await httpClient.put(`/api/resources/bar-assets/${id}`, data)
    return { ...response, data: normalizeAsset({ ...response?.data, ...data }) }
  },

  async deleteAsset(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    return httpClient.delete(`/api/resources/bar-assets/${id}`)
  },

  async getCertificates(params = {}) {
    if (isMockMode()) {
      const data = (mockData.barCertificates || []).map(normalizeCertificate)
      return Promise.resolve({ success: true, data, total: data.length })
    }
    const response = await httpClient.get('/api/resources/bar-certificates', { params })
    return {
      ...response,
      data: response?.data?.content ? response.data.content.map(normalizeCertificate) : (response?.data || []).map(normalizeCertificate),
      total: response?.data?.totalElements || response?.data?.length || 0,
    }
  },

  async getCertificate(id) {
    if (isMockMode()) {
      const item = (mockData.barCertificates || []).find((c) => String(c.id) === String(id))
      return Promise.resolve({ success: true, data: item ? normalizeCertificate(item) : null })
    }
    const response = await httpClient.get(`/api/resources/bar-certificates/${id}`)
    return { ...response, data: normalizeCertificate(response?.data) }
  },

  async createCertificate(data) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeCertificate({ ...data, id: `BC${Date.now()}` }),
      })
    }
    const response = await httpClient.post('/api/resources/bar-certificates', data)
    return { ...response, data: normalizeCertificate(response?.data) }
  },

  async updateCertificate(id, data) {
    if (isMockMode()) {
      return Promise.resolve({ success: true, data: normalizeCertificate({ ...data, id }) })
    }
    const response = await httpClient.put(`/api/resources/bar-certificates/${id}`, data)
    return { ...response, data: normalizeCertificate({ ...response?.data, ...data }) }
  },

  async deleteCertificate(id) {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    return httpClient.delete(`/api/resources/bar-certificates/${id}`)
  },

  async getCertificatesByAsset(assetId) {
    if (isMockMode()) {
      const data = (mockData.barCertificates || [])
        .filter((c) => String(c.assetId) === String(assetId))
        .map(normalizeCertificate)
      return Promise.resolve({ success: true, data })
    }
    const response = await httpClient.get(`/api/resources/bar-assets/${assetId}/certificates`)
    return {
      ...response,
      data: (response?.data || []).map(normalizeCertificate),
    }
  },
}

export default {
  barAssets: barAssetsApi,
}
