// Input: httpClient, auth response normalizers, and runtime settings persistence
// Output: authApi - authentication and current-user accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 认证模块 API
 * 真实 API 为唯一数据源
 */
import httpClient from '../client.js'
import { persistRuntimeSettings } from './settings.js'
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
  role: String(authPayload?.roleCode || authPayload?.role || '').toLowerCase(),
  roleCode: String(authPayload?.roleCode || authPayload?.role || '').toLowerCase(),
  roleName: authPayload?.roleName || '',
  dept: authPayload?.dept || authPayload?.departmentName || '',
  deptCode: authPayload?.deptCode || authPayload?.departmentCode || '',
  allowedProjectIds: normalizeAllowedProjectIds(authPayload?.allowedProjectIds),
  allowedDepts: normalizeAllowedDepts(authPayload?.allowedDepts),
  menuPermissions: Array.isArray(authPayload?.menuPermissions) ? authPayload.menuPermissions : []
})

export const getSavedUser = () => getStoredUser()

export const hasPersistentSession = () => hasPersistentUserHint()

export const clearAuthState = () => {
  clearSessionState()
}

export const authApi = {
  async login(username, password, rememberMe = true) {
    const response = await httpClient.post('/api/auth/login', { username, password, rememberMe }, {
      skipAuthRefresh: true
    })
    const authPayload = response?.data
    const normalizedUser = normalizeUser(authPayload)

    setAccessToken(authPayload?.token, rememberMe)
    persistRuntimeSettings({
      roles: [{
        code: normalizedUser.role,
        menuPermissions: normalizedUser.menuPermissions
      }]
    })

    return {
      ...response,
      data: {
        user: normalizedUser,
        token: authPayload?.token,
        type: authPayload?.type || 'Bearer'
      }
    }
  },

  async logout() {
    return httpClient.post('/api/auth/logout', null, {
      skipAuthRefresh: true,
      silentAuthError: true
    })
  },

  async getCurrentUser() {
    const response = await httpClient.get('/api/auth/me')
    const authPayload = response?.data
    const normalizedUser = normalizeUser(authPayload)

    persistRuntimeSettings({
      roles: [{
        code: normalizedUser.role,
        menuPermissions: normalizedUser.menuPermissions
      }]
    })

    return {
      ...response,
      data: normalizedUser
    }
  },

  async refreshToken() {
    const response = await httpClient.post('/api/auth/refresh', null, {
      skipAuthRefresh: true,
      silentAuthError: true
    })
    const authPayload = response?.data
    const normalizedUser = normalizeUser(authPayload)

    setAccessToken(authPayload?.token, true)
    persistRuntimeSettings({
      roles: [{
        code: normalizedUser.role,
        menuPermissions: normalizedUser.menuPermissions
      }]
    })

    return {
      ...response,
      data: {
        user: normalizedUser,
        token: authPayload?.token,
        type: authPayload?.type || 'Bearer'
      }
    }
  }
}

export default authApi
