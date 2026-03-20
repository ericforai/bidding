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
import {
  clearSessionState,
  getStoredUser,
  hasPersistentUserHint,
  setAccessToken
} from '../session.js'

const normalizeAllowedProjectIds = (allowedProjectIds) => {
  if (!Array.isArray(allowedProjectIds)) {
    return []
  }

  return [...new Set(allowedProjectIds.filter((projectId) => projectId !== null && projectId !== undefined))]
}

const normalizeAllowedDepts = (allowedDepts) => {
  if (!Array.isArray(allowedDepts)) {
    return []
  }

  return [...new Set(allowedDepts.filter((deptCode) => deptCode !== null && deptCode !== undefined && deptCode !== ''))]
}

const normalizeUser = (authPayload) => ({
  id: authPayload?.id,
  name: authPayload?.fullName || authPayload?.name || authPayload?.username,
  username: authPayload?.username,
  email: authPayload?.email,
  role: String(authPayload?.role || '').toLowerCase(),
  dept: authPayload?.dept || authPayload?.departmentName || '',
  deptCode: authPayload?.deptCode || authPayload?.departmentCode || '',
  allowedProjectIds: normalizeAllowedProjectIds(authPayload?.allowedProjectIds),
  allowedDepts: normalizeAllowedDepts(authPayload?.allowedDepts)
})

export const getSavedUser = () => getStoredUser()

export const hasPersistentSession = () => hasPersistentUserHint()

export const clearAuthState = () => {
  clearSessionState()
}

export const authApi = {
  /**
   * 用户登录
   */
  async login(username, password, rememberMe = true) {
    if (isMockMode()) {
      // Mock 模式
      return new Promise((resolve) => {
        setTimeout(() => {
          const user = mockData.users.find(u => u.name === username) || mockData.users[0]
          const token = 'mock-token-' + Date.now()
          setAccessToken(token)
          resolve({
            success: true,
            data: {
              user: {
                ...user,
                allowedProjectIds: normalizeAllowedProjectIds(user.allowedProjectIds)
              },
              token
            }
          })
        }, 300)
      })
    }

    // 真实 API 模式
    const response = await httpClient.post('/api/auth/login', { username, password, rememberMe }, {
      skipAuthRefresh: true
    })
    const authPayload = response?.data
    setAccessToken(authPayload?.token, rememberMe)

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
      clearAuthState()
      return Promise.resolve({ success: true })
    }
    return httpClient.post('/api/auth/logout', null, {
      skipAuthRefresh: true,
      silentAuthError: true
    })
  },

  /**
   * 获取当前用户信息
   */
  async getCurrentUser() {
    if (isMockMode()) {
      const savedUser = getSavedUser() || mockData.users[0]

      return Promise.resolve({
        success: true,
        data: {
          ...savedUser,
          allowedProjectIds: normalizeAllowedProjectIds(savedUser?.allowedProjectIds)
        }
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
  async refreshToken() {
    if (isMockMode()) {
      const user = getSavedUser()
      if (!user) {
        return Promise.resolve({
          success: false,
          message: 'No active mock session'
        })
      }
      const token = 'new-mock-token-' + Date.now()
      setAccessToken(token)
      return Promise.resolve({
        success: true,
        data: {
          user: {
            ...user,
            allowedProjectIds: normalizeAllowedProjectIds(user.allowedProjectIds)
          },
          token
        }
      })
    }
    const response = await httpClient.post('/api/auth/refresh', null, {
      skipAuthRefresh: true,
      silentAuthError: true
    })
    const authPayload = response?.data
    setAccessToken(authPayload?.token, true) // refresh 时默认持久化

    return {
      ...response,
      data: {
        user: normalizeUser(authPayload),
        token: authPayload?.token,
        type: authPayload?.type || 'Bearer'
      }
    }
  }
}

export default authApi
