// Input: bidding store, mocked tenders API
// Output: store state and action regression coverage
// Pos: stores/测试 - bidding store spec
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBiddingStore } from './bidding'
import { tendersApi } from '@/api'

// Mock API
vi.mock('@/api', () => ({
  tendersApi: {
    getList: vi.fn(),
    update: vi.fn()
  },
}))

describe('Bidding Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('初始状态应该正确', () => {
    const store = useBiddingStore()
    expect(store.tenders).toEqual([])
  })

  it('getters: highPriorityTenders 应该只返回评分 >= 85 的标讯', () => {
    const store = useBiddingStore()
    store.tenders = [
      { id: 1, aiScore: 90 },
      { id: 2, aiScore: 70 },
      { id: 3, aiScore: 85 }
    ]
    expect(store.highPriorityTenders).has.length(2)
    expect(store.highPriorityTenders.map(t => t.id)).contains(1, 3)
  })

  it('actions: getTenders 成功时应该更新 tenders', async () => {
    const store = useBiddingStore()
    const tenderRows = [{ id: 1, title: '测试标讯' }]
    tendersApi.getList.mockResolvedValue({ success: true, data: tenderRows })

    await store.getTenders()

    expect(store.tenders).toEqual(tenderRows)
    expect(tendersApi.getList).toHaveBeenCalledOnce()
  })

  it('actions: updateTenderStatus 应该调用 API 并刷新列表', async () => {
    const store = useBiddingStore()
    tendersApi.update.mockResolvedValue({ success: true })
    tendersApi.getList.mockResolvedValue({ success: true, data: [{ id: '100', status: 'TRACKING' }] })

    const result = await store.updateTenderStatus('100', 'TRACKING')

    expect(result).toEqual({ success: true })
    expect(tendersApi.update).toHaveBeenCalledWith('100', { status: 'TRACKING' })
    expect(tendersApi.getList).toHaveBeenCalledOnce()
    expect(store.tenders[0].status).toBe('TRACKING')
  })

  it('getters: newTenders 应该匹配 PENDING 状态', () => {
    const store = useBiddingStore()
    store.tenders = [
      { id: 1, status: 'PENDING' },
      { id: 2, status: 'TRACKING' },
      { id: 3, status: 'BIDDED' }
    ]
    expect(store.newTenders).toHaveLength(1)
    expect(store.newTenders[0].id).toBe(1)
    expect(store.followingTenders).toHaveLength(1)
    expect(store.followingTenders[0].id).toBe(2)
    expect(store.biddingTenders).toHaveLength(1)
    expect(store.biddingTenders[0].id).toBe(3)
  })
})
