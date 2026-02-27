import { defineStore } from 'pinia'
import { mockData } from '@/api/mock'

export const useUserStore = defineStore('user', {
  state: () => ({
    currentUser: null,
    users: mockData.users
  }),

  getters: {
    isLoggedIn: (state) => !!state.currentUser,
    userRole: (state) => state.currentUser?.role || 'guest',
    userName: (state) => state.currentUser?.name || '游客'
  },

  actions: {
    login(username, password) {
      this.currentUser = this.users.find(u => u.name === username) || this.users[0]
      // 保存到 localStorage 供路由守卫使用
      localStorage.setItem('user', JSON.stringify(this.currentUser))
      return Promise.resolve(this.currentUser)
    },

    logout() {
      this.currentUser = null
      localStorage.removeItem('user')
    },

    switchUser(userId) {
      this.currentUser = this.users.find(u => u.id === userId)
      localStorage.setItem('user', JSON.stringify(this.currentUser))
    }
  }
})
