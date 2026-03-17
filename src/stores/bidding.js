import { defineStore } from 'pinia'
import { mockData } from '@/api/mock'
import { tendersApi } from '@/api'

export const useBiddingStore = defineStore('bidding', {
  state: () => ({
    tenders: [],
    todos: mockData.todos,
    calendar: mockData.calendar
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
        } else if (!this.tenders.length) {
          this.tenders = [...mockData.tenders]
        }
      } catch (error) {
        // API 调用失败时回退到 mock 数据
        console.warn('API 调用失败，使用 mock 数据:', error.message)
        this.tenders = [...mockData.tenders]
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
      return this.todos
    },

    async updateTodoStatus(id, status) {
      const todo = this.todos.find(t => t.id === id)
      if (todo) {
        todo.status = status
      }
    },

    async getCalendar() {
      return this.calendar
    }
  }
})
