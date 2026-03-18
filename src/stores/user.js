// Input: authApi and demo user adapter for mock-mode session setup
// Output: useUserStore - Pinia store for authentication and current user state
// Pos: src/stores/ - State management layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { defineStore } from 'pinia'
import { authApi, isMockMode, settingsApi } from '@/api'
import { getDemoUsers } from '@/api/mock-adapters/frontendDemo.js'
import { clearAuthState, getStoredRefreshToken, getStoredToken, hasPersistentSession } from '@/api/modules/auth.js'
import { clearRuntimeSettings, getRolePermissionProfile } from '@/api/modules/settings.js'

// 从 localStorage 恢复用户状态
const getSavedUser = () => {
  try {
    const saved = localStorage.getItem('user') || sessionStorage.getItem('user')
    if (saved) {
      return JSON.parse(saved)
    }
  } catch (e) {
    console.error('Failed to parse saved user:', e)
  }
  return null
}

const getSavedToken = () => getStoredToken()
const getSavedRefreshToken = () => getStoredRefreshToken()

export const useUserStore = defineStore('user', {
  state: () => {
    const savedUser = getSavedUser()

    return {
      currentUser: savedUser,
      token: getSavedToken(),
      refreshToken: getSavedRefreshToken(),
      permissionProfileLoaded: false,
      users: getDemoUsers()
    }
  },

  getters: {
    isLoggedIn: (state) => !!state.currentUser && !!state.token,
    userRole: (state) => String(state.currentUser?.role || 'staff').toLowerCase(),
    userName: (state) => state.currentUser?.name || '用户',
    permissionProfile: (state) => getRolePermissionProfile(state.currentUser?.role)
  },

  actions: {
    async login(username, password, remember = true) {
      const result = await authApi.login(username, password)

      if (!result?.success || !result?.data?.user || !result?.data?.token) {
        throw new Error(result?.message || 'Login failed')
      }

      this.currentUser = result.data.user
      this.token = result.data.token
      this.refreshToken = result.data.refreshToken || null
      this.persistSession(remember)
      await this.refreshPermissionProfile()
      return this.currentUser
    },

    async restoreSession() {
      if (!this.token) {
        return null
      }

      if (this.currentUser) {
        return this.currentUser
      }

      const result = await authApi.getCurrentUser()
      if (!result?.success || !result?.data) {
        this.logout()
        return null
      }

      this.currentUser = result.data
      this.token = getStoredToken()
      this.refreshToken = getStoredRefreshToken()
      this.persistSession(hasPersistentSession())
      await this.refreshPermissionProfile()
      return this.currentUser
    },

    async logout() {
      try {
        await authApi.logout()
      } catch (error) {
        console.warn('Logout request failed, clearing local session anyway:', error)
      } finally {
        this.currentUser = null
        this.token = null
        this.refreshToken = null
        this.permissionProfileLoaded = false
        clearAuthState()
        clearRuntimeSettings()
      }
    },

    async refreshPermissionProfile() {
      if (isMockMode() || !this.token || !this.currentUser) {
        this.permissionProfileLoaded = false
        clearRuntimeSettings()
        return null
      }

      try {
        const response = await settingsApi.getRuntimePermissions()
        this.permissionProfileLoaded = Boolean(response?.success && response?.data)
        return response?.data || null
      } catch {
        this.permissionProfileLoaded = false
        clearRuntimeSettings()
        return null
      }
    },

    switchUser(userId) {
      this.currentUser = this.users.find(u => u.id === userId)
      if (this.currentUser && this.token) {
        this.persistSession(true)
      }
    },

    // 持久化用户状态到 storage
    persistSession(remember = true) {
      if (!this.currentUser || !this.token) return

      const storage = remember ? localStorage : sessionStorage
      const otherStorage = remember ? sessionStorage : localStorage

      otherStorage.removeItem('user')
      otherStorage.removeItem('token')
      otherStorage.removeItem('refreshToken')
      storage.setItem('user', JSON.stringify(this.currentUser))
      storage.setItem('token', this.token)
      if (this.refreshToken) {
        storage.setItem('refreshToken', this.refreshToken)
      } else {
        storage.removeItem('refreshToken')
      }
    }
  }
})
