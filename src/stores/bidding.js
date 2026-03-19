// Input: tendersApi, API mode switch, and frontend demo adapters for bidding views
// Output: useBiddingStore - Pinia store for tenders, todos, and calendar state
// Pos: src/stores/ - State management layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { defineStore } from 'pinia'
import { tendersApi, isMockMode } from '@/api'
import { getDemoCalendar, getDemoTodos } from '@/api/mock-adapters/frontendDemo.js'

export const useBiddingStore = defineStore('bidding', {
  state: () => ({
    tenders: [],
    todos: getDemoTodos(),
    calendar: getDemoCalendar()
  }),

  getters: {
    newTenders: (state) => state.tenders.filter(t => t.status === 'new'),
    followingTenders: (state) => state.tenders.filter(t => t.status === 'following'),
    biddingTenders: (state) => state.tenders.filter(t => t.status === 'bidding'),
    highPriorityTenders: (state) => state.tenders.filter(t => t.aiScore >= 85),
    urgentTodos: (state) => state.todos.filter(t => t.priority === 'high'),
    todayEvents: (state) => {
      const today = new Date().toISOString().split('T')[0]
      return state.calendar.filter(c => c.date === today)
    }
  },

  actions: {
    async getTenders(filters = {}) {
      try {
        const result = await tendersApi.getList(filters)
        if (result?.success) {
          this.tenders = result.data || []
        } else if (!isMockMode()) {
          this.tenders = []
        }
      } catch (error) {
        if (!isMockMode()) {
          console.warn('API 调用失败，返回空列表:', error.message)
          this.tenders = []
        }
      }
      return this.tenders
    },

    async updateTenderStatus(id, status) {
      const tender = this.tenders.find(t => String(t.id) === String(id))
      if (tender) {
        tender.status = status
      }
    },

    async getTodos() {
      if (!isMockMode()) {
        return []
      }
      return this.todos
    },

    async updateTodoStatus(id, status) {
      const todo = this.todos.find(t => t.id === id)
      if (todo) {
        todo.status = status
      }
    },

    async getCalendar() {
      if (!isMockMode()) {
        return []
      }
      return this.calendar
    }
  }
})
