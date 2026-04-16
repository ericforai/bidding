// Input: tendersApi
// Output: useBiddingStore - Pinia store for tender state and real API interactions
// Pos: src/stores/ - State management layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { defineStore } from 'pinia'
import { tendersApi } from '@/api'

export const useBiddingStore = defineStore('bidding', {
  state: () => ({
    tenders: []
  }),

  getters: {
    newTenders: (state) => state.tenders.filter(t => t.status === 'PENDING'),
    followingTenders: (state) => state.tenders.filter(t => t.status === 'TRACKING'),
    biddingTenders: (state) => state.tenders.filter(t => t.status === 'BIDDED'),
    highPriorityTenders: (state) => state.tenders.filter(t => t.aiScore >= 85)
  },

  actions: {
    async getTenders(filters = {}) {
      try {
        const result = await tendersApi.getList(filters)
        this.tenders = result?.success ? (result.data || []) : []
      } catch (error) {
        console.warn('API 调用失败，返回空列表:', error.message)
        this.tenders = []
      }
      return this.tenders
    },

    async updateTenderStatus(id, status) {
      const result = await tendersApi.update(id, { status })
      if (result?.success) {
        await this.getTenders()
      }
      return result
    }
  }
})
