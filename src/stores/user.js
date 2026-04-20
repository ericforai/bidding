// Input: authApi responses and demo user adapter for mock-mode session setup
// Output: useUserStore - Pinia store for auth, session restore, and user scope state
// Pos: src/stores/ - State management layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { getDemoUsers } from '@/api/mock-adapters/frontendDemo.js'
import { clearAuthState, hasPersistentSession } from '@/api/modules/auth.js'
import { persistRuntimeSettings } from '@/api/modules/settings.js'
import {
  bootstrapLegacyAccessToken,
  getStoredUser,
  persistUserHint
} from '@/api/session.js'

const navigateToLogin = async () => {
  const { default: router } = await import('@/router/index.js')
  if (router.currentRoute.value.path !== '/login') {
    await router.replace('/login')
  }
}

export const useUserStore = defineStore('user', {
  state: () => {
    const savedUser = getStoredUser()

    return {
      currentUser: savedUser,
      token: bootstrapLegacyAccessToken(),
      users: getDemoUsers(),
      isRestoringSession: false,
      hasRestoredSession: false
    }
  },

  getters: {
    isLoggedIn: (state) => !!state.currentUser && !!state.token,
    userRole: (state) => state.currentUser?.role || 'staff',
    userName: (state) => state.currentUser?.name || '用户',
    allowedProjectIds: (state) => state.currentUser?.allowedProjectIds || [],
    allowedDepts: (state) => state.currentUser?.allowedDepts || []
  },

  actions: {
    applyAuthSession(authData, remember = hasPersistentSession()) {
      const nextUser = authData?.user || authData
      const nextToken = authData?.token

      if (!nextUser) {
        return null
      }

      this.currentUser = nextUser
      if (nextToken) {
        this.token = nextToken
      }
      persistRuntimeSettings({
        roles: [{
          code: nextUser?.roleCode || nextUser?.role,
          name: nextUser?.roleName || '',
          menuPermissions: Array.isArray(nextUser?.menuPermissions) ? nextUser.menuPermissions : []
        }]
      })

      this.persistSession(remember)
      return this.currentUser
    },

    async login(username, password, remember = true) {
      const result = await authApi.login(username, password, remember)

      if (!result?.success || !result?.data?.user || !result?.data?.token) {
        throw new Error(result?.message || 'Login failed')
      }

      this.applyAuthSession(result.data, remember)
      this.hasRestoredSession = true
      return this.currentUser
    },

    async restoreSession() {
      if (this.isRestoringSession) {
        return this.currentUser
      }

      if (this.hasRestoredSession) {
        return this.currentUser
      }

      const hasProjectScopeSnapshot = Array.isArray(this.currentUser?.allowedProjectIds)
      const hasDeptScopeSnapshot = Array.isArray(this.currentUser?.allowedDepts)

      if (this.currentUser && this.token && hasProjectScopeSnapshot && hasDeptScopeSnapshot) {
        this.hasRestoredSession = true
        return this.currentUser
      }

      this.isRestoringSession = true

      try {
        const result = this.token
          ? await authApi.getCurrentUser()
          : await authApi.refreshToken()

        if (!result?.success || !result?.data) {
          throw new Error(result?.message || 'Session restore failed')
        }

        this.applyAuthSession(result.data, hasPersistentSession())
        return this.currentUser
      } catch (error) {
        this.resetSession()
        return null
      } finally {
        this.isRestoringSession = false
        this.hasRestoredSession = true
      }
    },

    async logout() {
      try {
        await authApi.logout()
      } catch (error) {
        console.warn('Logout request failed, clearing local session anyway:', error)
      } finally {
        this.resetSession()
        await navigateToLogin()
      }
    },

    resetSession() {
      this.currentUser = null
      this.token = null
      this.isRestoringSession = false
      this.hasRestoredSession = true
      clearAuthState()
    },

    // 持久化用户状态到 storage
    persistSession(remember = true) {
      if (!this.currentUser) return
      persistUserHint(this.currentUser, remember)
    }
  }
})
