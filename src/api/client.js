/**
 * HTTP 客户端封装
 * 基于 axios 实现，支持拦截器、错误处理、Token 管理
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_CONFIG } from './config'

const getStoredToken = () => localStorage.getItem('token') || sessionStorage.getItem('token')
const getStoredRefreshToken = () => localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken')

const getHeaderValue = (headers, headerName) => {
  if (!headers) {
    return null
  }

  const target = String(headerName).toLowerCase()
  const matchedKey = Object.keys(headers).find((key) => key.toLowerCase() === target)
  return matchedKey ? headers[matchedKey] : null
}

const getPreferredStorage = () => {
  if (
    sessionStorage.getItem('token') ||
    sessionStorage.getItem('refreshToken') ||
    sessionStorage.getItem('user')
  ) {
    return sessionStorage
  }
  if (
    localStorage.getItem('token') ||
    localStorage.getItem('refreshToken') ||
    localStorage.getItem('user')
  ) {
    return localStorage
  }
  return sessionStorage
}

const syncStoredTokens = ({ token, refreshToken }) => {
  const storage = getPreferredStorage()
  const otherStorage = storage === localStorage ? sessionStorage : localStorage

  if (token) {
    storage.setItem('token', token)
  }
  if (refreshToken) {
    storage.setItem('refreshToken', refreshToken)
  }

  if (token) {
    otherStorage.removeItem('token')
  }
  if (refreshToken) {
    otherStorage.removeItem('refreshToken')
  }
}

const clearStoredSession = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('refreshToken')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')
  sessionStorage.removeItem('refreshToken')
}

// 创建 axios 实例
const httpClient = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout,
  headers: API_CONFIG.headers
})

// 请求拦截器
httpClient.interceptors.request.use(
  (config) => {
    // 添加 Token
    const token = getStoredToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
httpClient.interceptors.response.use(
  (response) => {
    // 后端返回的统一格式: { success: true, data: ..., message: ... }
    return {
      ...response.data,
      _headers: response.headers
    }
  },
  async (error) => {
    const { response } = error
    const originalRequest = error.config || {}

    const shouldAttemptRefresh =
      response?.status === 401 &&
      !originalRequest._retry &&
      !String(originalRequest.url || '').includes('/api/auth/login') &&
      !String(originalRequest.url || '').includes('/api/auth/refresh') &&
      !String(originalRequest.url || '').includes('/api/auth/logout') &&
      Boolean(getStoredRefreshToken())

    if (shouldAttemptRefresh) {
      originalRequest._retry = true

      try {
        const refreshResponse = await axios.post(
          `${API_CONFIG.baseURL}/api/auth/refresh`,
          { refreshToken: getStoredRefreshToken() },
          {
            headers: {
              'Content-Type': 'application/json'
            },
            timeout: API_CONFIG.timeout
          }
        )

        const refreshedToken = refreshResponse?.data?.data?.token
        const rotatedRefreshToken =
          refreshResponse?.data?.data?.refreshToken ||
          getHeaderValue(refreshResponse?.headers, 'x-refresh-token')
        if (!refreshedToken) {
          throw new Error('Missing refreshed access token')
        }

        syncStoredTokens({
          token: refreshedToken,
          refreshToken: rotatedRefreshToken || getStoredRefreshToken()
        })

        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${refreshedToken}`
        return httpClient(originalRequest)
      } catch (refreshError) {
        clearStoredSession()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
        return Promise.reject(refreshError)
      }
    }

    if (response) {
      // 服务器返回错误状态码
      switch (response.status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          // 清除 token 并跳转登录
          clearStoredSession()
          if (window.location.pathname !== '/login') {
            window.location.href = '/login'
          }
          break
        case 403:
          ElMessage.error('没有权限访问该资源')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误，请稍后重试')
          break
        default:
          ElMessage.error(response.data?.message || '请求失败')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else {
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
    }

    return Promise.reject(error)
  }
)

export default httpClient
