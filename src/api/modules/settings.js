// Input: httpClient, API mode config, settings payload normalizers and fallback snapshots
// Output: settingsApi - admin settings accessors for data-scope configuration
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'
import { isMockMode } from '../config.js'

const fallbackConfig = {
  userDataScope: [],
  deptDataScope: [],
  deptOptions: [],
  deptTree: [],
  projectGroupScope: [],
  userOptions: []
}

const normalizeAllowedProjects = (allowedProjects) => {
  if (!Array.isArray(allowedProjects)) {
    return []
  }

  return [...new Set(
    allowedProjects
      .map((projectId) => Number(projectId))
      .filter((projectId) => Number.isFinite(projectId))
  )].sort((left, right) => left - right)
}

const normalizeAllowedDepts = (allowedDepts) => {
  if (!Array.isArray(allowedDepts)) {
    return []
  }

  return [...new Set(
    allowedDepts
      .map((deptCode) => String(deptCode || '').trim())
      .filter(Boolean)
  )]
}

const normalizeUserRow = (row = {}) => ({
  userId: row.userId,
  userName: row.userName || '',
  deptCode: row.deptCode || '',
  dept: row.dept || '',
  role: row.role || '',
  dataScope: row.dataScope || 'self',
  allowedProjects: normalizeAllowedProjects(row.allowedProjects),
  allowedDepts: normalizeAllowedDepts(row.allowedDepts)
})

const normalizeDeptRow = (row = {}) => ({
  deptCode: row.deptCode || '',
  deptName: row.deptName || '',
  dataScope: row.dataScope || 'self',
  canViewOtherDepts: Boolean(row.canViewOtherDepts),
  allowedDepts: normalizeAllowedDepts(row.allowedDepts)
})

const normalizeDeptOption = (option = {}) => ({
  code: option.code || '',
  name: option.name || ''
})

const normalizeDeptTreeItem = (item = {}) => ({
  deptCode: item.deptCode || '',
  deptName: item.deptName || '',
  parentDeptCode: item.parentDeptCode || '',
  sortOrder: Number.isFinite(Number(item.sortOrder)) ? Number(item.sortOrder) : 0
})

const normalizeProjectGroupRow = (row = {}) => ({
  groupCode: row.groupCode || '',
  groupName: row.groupName || '',
  managerUserId: row.managerUserId ?? null,
  manager: row.manager || '',
  memberCount: Number.isFinite(Number(row.memberCount)) ? Number(row.memberCount) : 0,
  visibility: row.visibility || 'members',
  memberUserIds: normalizeAllowedProjects(row.memberUserIds),
  allowedRoles: Array.isArray(row.allowedRoles) ? [...new Set(row.allowedRoles.map((role) => String(role || '').trim()).filter(Boolean))] : [],
  projectIds: normalizeAllowedProjects(row.projectIds)
})

const normalizeUserOption = (option = {}) => ({
  id: option.id,
  name: option.name || '',
  role: option.role || '',
  deptCode: option.deptCode || '',
  dept: option.dept || ''
})

const normalizeConfig = (payload = fallbackConfig) => ({
  userDataScope: Array.isArray(payload.userDataScope) ? payload.userDataScope.map(normalizeUserRow) : [],
  deptDataScope: Array.isArray(payload.deptDataScope) ? payload.deptDataScope.map(normalizeDeptRow) : [],
  deptOptions: Array.isArray(payload.deptOptions) ? payload.deptOptions.map(normalizeDeptOption) : [],
  deptTree: Array.isArray(payload.deptTree) ? payload.deptTree.map(normalizeDeptTreeItem) : [],
  projectGroupScope: Array.isArray(payload.projectGroupScope) ? payload.projectGroupScope.map(normalizeProjectGroupRow) : [],
  userOptions: Array.isArray(payload.userOptions) ? payload.userOptions.map(normalizeUserOption) : []
})

export const settingsApi = {
  async getDataScopeConfig() {
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizeConfig(fallbackConfig)
      })
    }

    const response = await httpClient.get('/api/admin/settings/data-scope')
    return {
      ...response,
      data: normalizeConfig(response?.data)
    }
  },

  async saveDataScopeConfig(payload) {
    const normalizedPayload = normalizeConfig(payload)
    if (isMockMode()) {
      return Promise.resolve({
        success: true,
        data: normalizedPayload
      })
    }

    const response = await httpClient.put('/api/admin/settings/data-scope', normalizedPayload)
    return {
      ...response,
      data: normalizeConfig(response?.data)
    }
  }
}

export default settingsApi
