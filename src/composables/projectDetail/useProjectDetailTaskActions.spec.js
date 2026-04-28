import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import { useProjectDetailTaskActions } from './useProjectDetailTaskActions.js'
import { useProjectDetailTasks } from './useProjectDetailTasks.js'

describe('useProjectDetailTaskActions', () => {
  it('API 项目点击拆解任务调用后端拆解接口并写入任务，不打开评分弹窗', async () => {
    const success = vi.fn()
    const error = vi.fn()
    const decomposeTasks = vi.fn().mockResolvedValue({
      success: true,
      data: [
        {
          id: 501,
          name: '资格文件整理',
          status: 'todo',
          deliverables: null,
          hasDeliverable: false,
        },
      ],
    })
    const state = {
      project: ref({ id: 12, name: '测试项目', tasks: [] }),
      activities: ref([]),
      scoreDraftDialogVisible: ref(false),
      currentTask: ref(null),
      taskDialogVisible: ref(false),
    }

    const { handleGenerateTasks } = useProjectDetailTaskActions({
      route: { params: { id: '12' } },
      userStore: { userName: '测试用户', currentUser: { id: 9 } },
      projectStore: {},
      projectsApi: { decomposeTasks },
      isApiProject: ref(true),
      message: { success, error, warning: vi.fn() },
      state,
      workflow: {},
    })

    await handleGenerateTasks()

    expect(decomposeTasks).toHaveBeenCalledWith('12')
    expect(state.scoreDraftDialogVisible.value).toBe(false)
    expect(state.project.value.tasks).toEqual([
      expect.objectContaining({
        id: 501,
        name: '资格文件整理',
        deliverables: [],
        hasDeliverable: false,
      }),
    ])
    expect(success).toHaveBeenCalledWith('已拆解生成 1 个任务')
    expect(error).not.toHaveBeenCalled()
  })

  it('API 项目上传标书拆解文件后调用独立拆解接口，不启动 AI 初稿', async () => {
    const file = new File(['招标正文'], '招标文件.docx')
    const success = vi.fn()
    const projectsApi = {
      parseTenderBreakdown: vi.fn().mockResolvedValue({
        success: true,
        data: { document: { snapshotId: 601 } },
      }),
    }
    const { handleTenderBreakdownUpload } = useProjectDetailTaskActions({
      route: { params: { id: 12 } },
      userStore: { userName: '小王' },
      projectStore: {},
      projectsApi,
      isApiProject: { value: true },
      message: { success, error: vi.fn(), warning: vi.fn() },
      state: {
        project: { value: { id: 12, tasks: [] } },
        activities: { value: [] },
        tenderBreakdownDialogVisible: { value: true },
        tenderBreakdownParsing: { value: false },
      },
      workflow: {},
    })

    const result = await handleTenderBreakdownUpload(file)

    expect(result).toBe(false)
    expect(projectsApi.parseTenderBreakdown).toHaveBeenCalledWith(12, file)
    expect(success).toHaveBeenCalledWith('招标文件已拆解，可继续生成任务或标书初稿')
  })

  it('API 项目解析中重复上传标书文件时不再次调用后端', async () => {
    const file = new File(['招标正文'], '招标文件.docx')
    const warning = vi.fn()
    const projectsApi = {
      parseTenderBreakdown: vi.fn(),
    }
    const { handleTenderBreakdownUpload } = useProjectDetailTaskActions({
      route: { params: { id: 12 } },
      userStore: { userName: '小王' },
      projectStore: {},
      projectsApi,
      isApiProject: { value: true },
      message: { success: vi.fn(), error: vi.fn(), warning },
      state: {
        project: { value: { id: 12, tasks: [] } },
        activities: { value: [] },
        tenderBreakdownDialogVisible: { value: true },
        tenderBreakdownParsing: { value: true },
      },
      workflow: {},
    })

    const result = await handleTenderBreakdownUpload(file)

    expect(result).toBe(false)
    expect(projectsApi.parseTenderBreakdown).not.toHaveBeenCalled()
    expect(warning).toHaveBeenCalledWith('正在解析招标文件，请稍候')
  })

  it('API 项目拆解无来源时只展示一次后端业务错误', async () => {
    const success = vi.fn()
    const error = vi.fn()
    const decomposeTasks = vi.fn().mockRejectedValue({
      message: 'Request failed with status code 400',
      response: {
        data: {
          message: '未找到可用于拆解任务的标书拆解结果',
        },
      },
    })
    const state = {
      project: ref({ id: 12, name: '测试项目', tasks: [] }),
      activities: ref([]),
      scoreDraftDialogVisible: ref(false),
      currentTask: ref(null),
      taskDialogVisible: ref(false),
    }

    const { handleGenerateTasks } = useProjectDetailTaskActions({
      route: { params: { id: '12' } },
      userStore: { userName: '测试用户', currentUser: { id: 9 } },
      projectStore: {},
      projectsApi: { decomposeTasks },
      isApiProject: ref(true),
      message: { success, error, warning: vi.fn() },
      state,
      workflow: {},
    })

    await handleGenerateTasks()

    expect(error).toHaveBeenCalledTimes(1)
    expect(error).toHaveBeenCalledWith('未找到可用于拆解任务的标书拆解结果')
    expect(success).not.toHaveBeenCalled()
  })

  it('legacy detail task composable reuses real API task decomposition', async () => {
    const success = vi.fn()
    const error = vi.fn()
    const decomposeTasks = vi.fn().mockResolvedValue({
      success: true,
      data: [{ id: 701, name: '商务标：商务响应', deliverables: null }],
    })
    const context = {
      project: ref({ id: 12, name: '测试项目', tasks: [] }),
      route: { params: { id: '12' } },
      userStore: { userName: '测试用户', currentUser: { id: 9 } },
      projectStore: {},
      projectsApi: { decomposeTasks },
      isApiProject: ref(true),
      message: { success, error, warning: vi.fn() },
      activities: ref([]),
      scoreDraftDialogVisible: ref(false),
      currentTask: ref(null),
      taskDialogVisible: ref(false),
      deliverableTypeMap: {},
      handleInitiateProcess: vi.fn(),
    }

    const { handleGenerateTasks } = useProjectDetailTasks(context)

    await handleGenerateTasks()

    expect(decomposeTasks).toHaveBeenCalledWith('12')
    expect(context.scoreDraftDialogVisible.value).toBe(false)
    expect(context.project.value.tasks).toEqual([
      expect.objectContaining({
        id: 701,
        name: '商务标：商务响应',
        deliverables: [],
      }),
    ])
    expect(success).toHaveBeenCalledWith('已拆解生成 1 个任务')
    expect(error).not.toHaveBeenCalled()
  })
})
