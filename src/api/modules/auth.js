/**
 * 认证模块 API
 * 支持双模式切换
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'
import { buildFeatureUnavailableResponse } from '../featureAvailability.js'

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
              token: 'mock-token-' + Date.now()
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
    return Promise.resolve(buildFeatureUnavailableResponse({
      feature: 'auth-logout',
      title: '退出登录接口暂未接入',
      message: 'Logout endpoint is not implemented on backend',
      hint: '当前前端仍会清理本地会话，但不会调用后端登出接口。',
      scope: 'action',
    }))
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
        data: { token: 'new-mock-token-' + Date.now() }
      })
    }
    return Promise.resolve(buildFeatureUnavailableResponse({
      feature: 'auth-refresh-token',
      title: '刷新令牌接口暂未接入',
      message: 'Refresh token endpoint is not implemented on backend',
      hint: '当前真实联调依赖重新登录获取新会话。',
      scope: 'action',
    }))
  }
}

export default authApi
