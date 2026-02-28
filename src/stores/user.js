import { defineStore } from 'pinia'
import { mockData } from '@/api/mock'

// 从 localStorage 恢复用户状态
const getSavedUser = () => {
  try {
    const saved = localStorage.getItem('user')
    if (saved) {
      return JSON.parse(saved)
    }
  } catch (e) {
    console.error('Failed to parse saved user:', e)
  }
  return null
}

export const useUserStore = defineStore('user', {
  state: () => {
    // 企业内部系统，默认登录为小王账号
    const defaultUser = mockData.users.find(u => u.name === '小王') || mockData.users[0]
    const savedUser = getSavedUser()

    return {
      // 如果有保存的用户则使用保存的，否则使用默认的小王账号
      currentUser: savedUser || defaultUser,
      users: mockData.users
    }
  },

  getters: {
    isLoggedIn: (state) => !!state.currentUser,
    userRole: (state) => state.currentUser?.role || 'staff',
    userName: (state) => state.currentUser?.name || '用户'
  },

  actions: {
    login(username, password) {
      this.currentUser = this.users.find(u => u.name === username) || this.users[0]
      // 保存到 localStorage
      this.persistUser()
      return Promise.resolve(this.currentUser)
    },

    logout() {
      this.currentUser = null
      localStorage.removeItem('user')
    },

    switchUser(userId) {
      this.currentUser = this.users.find(u => u.id === userId)
      this.persistUser()
    },

    // 持久化用户状态到 localStorage
    persistUser() {
      if (this.currentUser) {
        localStorage.setItem('user', JSON.stringify(this.currentUser))
      }
    }
  }
})
