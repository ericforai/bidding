import { defineStore } from 'pinia'
import { mockData } from '@/api/mock'

export const useBiddingStore = defineStore('bidding', {
  state: () => ({
    tenders: mockData.tenders,
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
    async getTenders() {
      return this.tenders
    },

    async updateTenderStatus(id, status) {
      const tender = this.tenders.find(t => t.id === id)
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
