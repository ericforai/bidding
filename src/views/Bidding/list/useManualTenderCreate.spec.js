import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useManualTenderCreate } from './useManualTenderCreate.js'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
  },
}))

function createWorkflow(overrides = {}) {
  const tendersApi = {
    create: vi.fn(),
  }
  const workflow = useManualTenderCreate({
    tendersApi,
    refreshTenderList: vi.fn(),
    canCreateTender: ref(true),
    ...overrides,
  })
  return { workflow, tendersApi }
}

describe('useManualTenderCreate', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('submits reviewed manual form values when framework agreement has no budget', async () => {
    const refreshTenderList = vi.fn()
    const { workflow, tendersApi } = createWorkflow({ refreshTenderList })
    tendersApi.create.mockResolvedValue({ success: true })
    workflow.manualFormRef.value = { validate: vi.fn().mockResolvedValue(true) }
    workflow.manualForm.value = {
      ...workflow.manualForm.value,
      title: '中国兵器装备集团有限公司电子商城电商供应商引入项目',
      budget: null,
      region: '北京',
      industry: '电商',
      deadline: new Date('2026-10-14T00:00:00'),
      purchaser: '南方工业科技贸易有限公司',
      contact: '',
      phone: '',
      description: '框架协议供应商引入，无明确采购预算。',
      tags: ['框架协议'],
    }

    await expect(workflow.saveManualTender()).resolves.toBe(true)

    expect(tendersApi.create).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '中国兵器装备集团有限公司电子商城电商供应商引入项目',
        budget: null,
        region: '北京',
        industry: '电商',
        purchaserName: '南方工业科技贸易有限公司',
        status: 'PENDING',
      }),
    )
    expect(refreshTenderList).toHaveBeenCalled()
  })
})
