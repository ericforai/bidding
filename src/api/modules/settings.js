// Input: httpClient
// Output: settingsApi - system settings retrieval and update functions
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'

const normalizeRole = (role) => String(role || '').trim().toLowerCase()
const normalizePermissionList = (permissions) => (
  Array.isArray(permissions)
    ? [...new Set(permissions.map(item => String(item || '').trim()).filter(Boolean))]
    : []
)

let runtimeSettings = null

const buildRuntimeRoleMap = (payload) => {
  const roles = Array.isArray(payload?.roles) ? payload.roles : []
  return roles.reduce((acc, role) => {
    const code = normalizeRole(role?.code)
    if (!code) return acc

    acc[code] = {
      code,
      menuPermissions: normalizePermissionList(role?.menuPermissions),
      dataScope: role?.dataScope || 'self',
      allowedProjects: Array.isArray(role?.allowedProjects) ? [...role.allowedProjects] : [],
      allowedDepts: Array.isArray(role?.allowedDepts) ? [...role.allowedDepts] : []
    }
    return acc
  }, {})
}

export const persistRuntimeSettings = (payload) => {
  if (!payload) return null

  runtimeSettings = {
    updatedAt: Date.now(),
    roleMap: buildRuntimeRoleMap(payload)
  }
  return runtimeSettings
}

export const clearRuntimeSettings = () => {
  runtimeSettings = null
}

export const getRuntimeSettings = () => runtimeSettings

export const getRolePermissionProfile = (role) => {
  const runtimeSettings = getRuntimeSettings()
  if (!runtimeSettings) return null

  return runtimeSettings.roleMap?.[normalizeRole(role)] || null
}

export const hasMenuAccessForRole = (role, permissionKeys = []) => {
  const profile = getRolePermissionProfile(role)
  if (!profile) return null

  const normalizedKeys = normalizePermissionList(permissionKeys)
  if (normalizedKeys.length === 0) return true

  if (profile.menuPermissions.includes('all')) return true
  return normalizedKeys.some(key => profile.menuPermissions.includes(key))
}

export const settingsApi = {
  async getSettings() {
    const response = await httpClient.get('/api/settings')
    if (response?.success && response?.data) {
      persistRuntimeSettings(response.data)
    }
    return response
  },

  async updateSettings(payload) {
    const response = await httpClient.put('/api/settings', payload)
    if (response?.success && response?.data) {
      persistRuntimeSettings(response.data)
    }
    return response
  },

  async getRuntimePermissions() {
    const response = await httpClient.get('/api/settings/runtime-permissions')
    if (response?.success && response?.data) {
      runtimeSettings = {
        updatedAt: Date.now(),
        roleMap: {
          [normalizeRole(response.data.code)]: {
            code: normalizeRole(response.data.code),
            menuPermissions: normalizePermissionList(response.data.menuPermissions),
            dataScope: response.data.dataScope || 'self',
            allowedProjects: Array.isArray(response.data.allowedProjects) ? [...response.data.allowedProjects] : [],
            allowedDepts: Array.isArray(response.data.allowedDepts) ? [...response.data.allowedDepts] : []
          }
        }
      }
    }
    return response
  }
}

export default settingsApi
