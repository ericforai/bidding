/**
 * API 配置文件
 * 支持双模式切换：Mock 数据 / 真实后端 API
 */

// 环境变量或手动控制
// 兼容 Node.js 和 Vite 环境
const viteEnv = typeof import.meta !== 'undefined' && import.meta.env ? import.meta.env : {}
const API_MODE = viteEnv.VITE_API_MODE || process.env.VITE_API_MODE || 'mock' // 'mock' | 'api'
const APP_PORT = viteEnv.VITE_APP_PORT || process.env.VITE_APP_PORT || (API_MODE === 'api' ? '1818' : '1314')

// 后端 API 基础地址
export const API_BASE_URL = viteEnv.VITE_API_BASE_URL || process.env.VITE_API_BASE_URL || 'http://localhost:8080'

// API 模式
export const API_CONFIG = {
  mode: API_MODE, // mock | api
  appPort: APP_PORT,
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
}

// 是否使用 Mock 数据
export const isMockMode = () => API_CONFIG.mode === 'mock'

// 商用品正式交付模式以真实 API 为唯一事实源
export const isCommercialMode = () => API_CONFIG.mode === 'api'

// 获取完整的 API URL
export const getApiUrl = (path) => {
  return `${API_BASE_URL}${path}`
}

// 导出配置供其他模块使用
export default API_CONFIG
