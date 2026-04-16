/**
 * API 配置文件
 * 真实 API 为唯一数据源
 */

const viteEnv = typeof import.meta !== 'undefined' && import.meta.env ? import.meta.env : {}

export const API_BASE_URL = viteEnv.VITE_API_BASE_URL || process.env.VITE_API_BASE_URL || 'http://localhost:18080'

export const API_CONFIG = {
  mode: 'api',
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
}

export const isCommercialMode = () => true

export const getApiUrl = (path) => `${API_BASE_URL}${path}`

export default API_CONFIG
