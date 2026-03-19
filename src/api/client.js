/**
 * HTTP 客户端封装
 * 基于 axios 实现，支持拦截器、自动刷新和会话状态同步
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_CONFIG } from './config'
import {
  bootstrapLegacyAccessToken,
  clearSessionState,
  getAccessToken
} from './session.js'
import router from '@/router/index.js'

let refreshPromise = null

const syncRefreshedSession = async (refreshResult) => {
  if (!refreshResult?.success || !refreshResult?.data?.user) {
    return
  }

  try {
    const { useUserStore } = await import('@/stores/user')
    const userStore = useUserStore()
    userStore.applyAuthSession(refreshResult.data)
  } catch (syncError) {
    console.warn('Failed to sync refreshed auth session:', syncError)
  }
}

const shouldSkipRefresh = (config = {}) => {
  const url = String(config.url || '')
  return Boolean(
    config.skipAuthRefresh ||
    url.includes('/api/auth/login') ||
    url.includes('/api/auth/refresh') ||
    url.includes('/api/auth/logout')
  )
}

// 创建 axios 实例
const httpClient = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout,
  headers: API_CONFIG.headers,
  withCredentials: true
})

// 请求拦截器
httpClient.interceptors.request.use(
  (config) => {
    // 添加 Token
    const token = getAccessToken() || bootstrapLegacyAccessToken()
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
    return response.data
  },
  async (error) => {
    const { response, config } = error

    if (response?.status === 401 && config && !config._retry && !shouldSkipRefresh(config)) {
      config._retry = true

      try {
        if (!refreshPromise) {
          refreshPromise = import('./modules/auth.js')
            .then(({ authApi }) => authApi.refreshToken())
            .finally(() => {
              refreshPromise = null
            })
        }

        const refreshResult = await refreshPromise
        await syncRefreshedSession(refreshResult)
        const token = refreshResult?.data?.token || getAccessToken()

        if (token) {
          config.headers = config.headers || {}
          config.headers.Authorization = `Bearer ${token}`
        }

        return httpClient(config)
      } catch (refreshError) {
        // Refresh 失败 - 清除会话并跳转到登录页
        ElMessage.error('登录已过期，请重新登录')
        clearSessionState()
        // Also clear Pinia store state to ensure route guard sees correct state
        const { useUserStore } = await import('@/stores/user.js')
        useUserStore().resetSession()
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login').catch((navError) => {
            if (navError.name !== 'NavigationDuplicated') {
              console.error('Navigation to login failed:', navError)
            }
          })
        }
        // 抛出错误以中止原请求
        throw refreshError
      }
    }

    if (response) {
      // 服务器返回错误状态码
      switch (response.status) {
        case 401:
          if (!shouldSkipRefresh(config)) {
            ElMessage.error('登录已过期，请重新登录')
            clearSessionState()
            // Use Vue Router for navigation to ensure guards are triggered
            if (router.currentRoute.value.path !== '/login') {
              router.push('/login').catch((navError) => {
              // Ignore navigation aborted errors (e.g., user navigated away)
                if (navError.name !== 'NavigationDuplicated') {
                  console.error('Navigation to login failed:', navError)
                }
              })
            }
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
