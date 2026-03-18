// Input: authApi and demo user adapter for mock-mode session setup
// Output: useUserStore - Pinia store for authentication and current user state
// Pos: src/stores/ - State management layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { getDemoUsers } from '@/api/mock-adapters/frontendDemo.js'

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

const getSavedToken = () => localStorage.getItem('token') || sessionStorage.getItem('token')

export const useUserStore = defineStore('user', {
  state: () => {
    const savedUser = getSavedUser()

    return {
      currentUser: savedUser,
      token: getSavedToken(),
      users: getDemoUsers()
    }
  },

  getters: {
    isLoggedIn: (state) => !!state.currentUser && !!state.token,
    userRole: (state) => state.currentUser?.role || 'staff',
    userName: (state) => state.currentUser?.name || '用户'
  },

  actions: {
    async login(username, password, remember = true) {
      const result = await authApi.login(username, password)

      if (!result?.success || !result?.data?.user || !result?.data?.token) {
        throw new Error(result?.message || 'Login failed')
      }

      this.currentUser = result.data.user
      this.token = result.data.token
      this.persistSession(remember)
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
      this.persistSession(Boolean(localStorage.getItem('token')))
      return this.currentUser
    },

    logout() {
      this.currentUser = null
      this.token = null
      localStorage.removeItem('user')
      localStorage.removeItem('token')
      sessionStorage.removeItem('user')
      sessionStorage.removeItem('token')
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
      storage.setItem('user', JSON.stringify(this.currentUser))
      storage.setItem('token', this.token)
    }
  }
})
