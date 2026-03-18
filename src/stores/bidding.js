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
    todayEvents: (state) => state.calendar.filter(c => c.date === '2025-02-26')
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
