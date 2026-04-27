import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useManualTenderCreate } from './useManualTenderCreate.js'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
  },
}))

import { ElMessage } from 'element-plus'

function createWorkflow(overrides = {}) {
  const tendersApi = {
    create: vi.fn(),
    parseTenderIntakeDocument: vi.fn(),
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

  it('backfills editable manual form fields after a supported attachment is parsed', async () => {
    const { workflow, tendersApi } = createWorkflow()
    const file = new File(['tender'], '招标文件.pdf', { type: 'application/pdf' })
    tendersApi.parseTenderIntakeDocument.mockResolvedValue({
      success: true,
      data: {
        extractedData: {
          tenderTitle: '西域智能仓储采购项目',
          budget: '1200000',
          region: '上海',
          industry: '数据中心',
          deadline: '2026-06-01T17:00:00',
          purchaserName: '西域采购中心',
          contactName: '李经理',
          tenderScope: '仓储自动化设备采购',
          tags: ['公开招标', '智能仓储'],
        },
      },
    })

    await workflow.handleFileChange({ name: file.name, raw: file }, [{ name: file.name, raw: file }])

    expect(tendersApi.parseTenderIntakeDocument).toHaveBeenCalledWith(file, { entityId: 'manual-tender' })
    expect(workflow.manualForm.value).toMatchObject({
      title: '西域智能仓储采购项目',
      budget: 1200000,
      region: '上海',
      industry: '数据中心',
      purchaser: '西域采购中心',
      contact: '李经理',
      description: '仓储自动化设备采购',
      tags: ['公开招标', '智能仓储'],
    })
    expect(workflow.manualForm.value.deadline).toEqual(new Date('2026-06-01T17:00:00'))
    expect(ElMessage.success).toHaveBeenCalledWith('DeepSeek/AI 已识别附件内容，可继续编辑后保存')
  })

  it('keeps existing manual values when document parsing fails', async () => {
    const { workflow, tendersApi } = createWorkflow()
    workflow.manualForm.value.title = '手动输入标题'
    workflow.manualForm.value.budget = 300000
    const file = new File(['bad'], '坏文件.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    tendersApi.parseTenderIntakeDocument.mockRejectedValue(new Error('parse failed'))

    await workflow.handleFileChange({ name: file.name, raw: file }, [{ name: file.name, raw: file }])

    expect(workflow.manualForm.value.title).toBe('手动输入标题')
    expect(workflow.manualForm.value.budget).toBe(300000)
    expect(ElMessage.warning).toHaveBeenCalledWith('自动识别失败，可继续手动填写')
  })

  it('clears parsing state and shows timeout hint when AI parsing times out', async () => {
    const { workflow, tendersApi } = createWorkflow()
    const file = new File(['slow'], '慢文件.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    tendersApi.parseTenderIntakeDocument.mockRejectedValue({ code: 'ECONNABORTED' })

    await workflow.handleFileChange({ name: file.name, raw: file }, [{ name: file.name, raw: file }])

    expect(workflow.parsingManualDocument.value).toBe(false)
    expect(ElMessage.warning).toHaveBeenCalledWith('AI 解析超时，可继续手动填写')
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
