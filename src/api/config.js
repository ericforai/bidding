/**
 * API 配置文件
 * 支持双模式切换：Mock 数据 / 真实后端 API
 */

// 环境变量或手动控制
// 兼容 Node.js 和 Vite 环境
const viteEnv = typeof import.meta !== 'undefined' && import.meta.env ? import.meta.env : {}
const API_MODE = viteEnv.VITE_API_MODE || process.env.VITE_API_MODE || 'api' // 'mock' | 'api'
const ENABLE_MOCK_LOGIN = (viteEnv.VITE_ENABLE_MOCK_LOGIN || process.env.VITE_ENABLE_MOCK_LOGIN || 'false') === 'true'
const ENABLE_TENDER_API_MOCK_FALLBACK = (viteEnv.VITE_API_ALLOW_TENDER_MOCK_FALLBACK || process.env.VITE_API_ALLOW_TENDER_MOCK_FALLBACK || 'false') === 'true'

// 后端 API 基础地址
export const API_BASE_URL = viteEnv.VITE_API_BASE_URL || process.env.VITE_API_BASE_URL || 'http://localhost:8080'

// API 模式
export const API_CONFIG = {
  mode: API_MODE, // mock | api
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
}

// 是否使用 Mock 数据
export const isMockMode = () => API_CONFIG.mode === 'mock'
export const isMockLoginEnabled = () => isMockMode() && ENABLE_MOCK_LOGIN
export const isTenderApiMockFallbackEnabled = () => !isMockMode() && ENABLE_TENDER_API_MOCK_FALLBACK

// 商用品正式交付模式以真实 API 为唯一事实源
export const isCommercialMode = () => API_CONFIG.mode === 'api'

// 获取完整的 API URL
export const getApiUrl = (path) => {
  return `${API_BASE_URL}${path}`
}

// 导出配置供其他模块使用
export default API_CONFIG
