// Input: httpClient, API mode config, auth response normalizers and demo users
// Output: authApi - authentication and current-user accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 认证模块 API
 * 支持双模式切换
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

const normalizeUser = (authPayload) => ({
  id: authPayload?.id,
  name: authPayload?.fullName || authPayload?.username,
  username: authPayload?.username,
  email: authPayload?.email,
  role: String(authPayload?.role || '').toLowerCase()
})

const getSavedUser = () => {
  const userStr = localStorage.getItem('user') || sessionStorage.getItem('user')
  return userStr ? JSON.parse(userStr) : null
}

export const getStoredToken = () => localStorage.getItem('token') || sessionStorage.getItem('token')

export const getStoredRefreshToken = () => localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken')

export const hasPersistentSession = () => Boolean(localStorage.getItem('token'))

export const clearAuthState = () => {
  localStorage.removeItem('user')
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  sessionStorage.removeItem('user')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('refreshToken')
}

const extractRefreshToken = (response) => {
  if (response?.data?.refreshToken) {
    return response.data.refreshToken
  }

  const headers = response?._headers
  if (!headers) {
    return null
  }

  const matchedKey = Object.keys(headers).find((key) => key.toLowerCase() === 'x-refresh-token')
  return matchedKey ? headers[matchedKey] : null
}

export const authApi = {
  /**
   * 用户登录
   */
  async login(username, password) {
    if (isMockMode()) {
      // Mock 模式
      return new Promise((resolve) => {
        setTimeout(() => {
          const user = mockData.users.find(u => u.name === username) || mockData.users[0]
          resolve({
            success: true,
            data: {
              user,
              token: 'mock-token-' + Date.now(),
              refreshToken: 'mock-refresh-token-' + Date.now()
            }
          })
        }, 300)
      })
    }

    // 真实 API 模式
    const response = await httpClient.post('/api/auth/login', { username, password })
    const authPayload = response?.data

    return {
      ...response,
      data: {
        user: normalizeUser(authPayload),
        token: authPayload?.token,
        refreshToken: authPayload?.refreshToken || extractRefreshToken(response),
        type: authPayload?.type || 'Bearer'
      }
    }
  },

  /**
   * 用户登出
   */
  async logout() {
    if (isMockMode()) {
      return Promise.resolve({ success: true })
    }
    const refreshToken = getStoredRefreshToken()
    return httpClient.post('/api/auth/logout', refreshToken ? { refreshToken } : {})
  },

  /**
   * 获取当前用户信息
   */
  async getCurrentUser() {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: getSavedUser() || mockData.users[0]
      })
    }

    const response = await httpClient.get('/api/auth/me')
    const authPayload = response?.data

    return {
      ...response,
      data: normalizeUser(authPayload)
    }
  },

  /**
   * 刷新 Token
   */
  async refreshToken(refreshToken) {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: {
          token: 'new-mock-token-' + Date.now(),
          refreshToken: 'new-mock-refresh-token-' + Date.now()
        }
      })
    }
    const refreshTokenValue = refreshToken || getStoredRefreshToken()
    const response = await httpClient.post('/api/auth/refresh', refreshTokenValue ? { refreshToken: refreshTokenValue } : {})
    const authPayload = response?.data

    return {
      ...response,
      data: {
        user: normalizeUser(authPayload),
        token: authPayload?.token,
        refreshToken: authPayload?.refreshToken || extractRefreshToken(response),
        type: authPayload?.type || 'Bearer'
      }
    }
  }
}

export default authApi
