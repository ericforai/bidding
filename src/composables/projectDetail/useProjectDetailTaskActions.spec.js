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
      getTenderBreakdownReadiness: vi.fn().mockResolvedValue({
        success: true,
        data: { ready: true },
      }),
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
    expect(projectsApi.getTenderBreakdownReadiness).toHaveBeenCalledWith(12)
    expect(projectsApi.parseTenderBreakdown).toHaveBeenCalledWith(12, file)
    expect(success).toHaveBeenCalledWith('招标文件已拆解，可继续生成任务或标书初稿')
  })

  it('API 项目已有解析快照时点击解析入口直接复用，不打开上传弹窗', async () => {
    const success = vi.fn()
    const getLatestTenderBreakdown = vi.fn().mockResolvedValue({
      success: true,
      data: {
        document: {
          name: '招标文件.docx',
          snapshotId: 601,
        },
      },
    })
    const state = {
      project: { value: { id: 12, tasks: [] } },
      activities: { value: [] },
      tenderBreakdownDialogVisible: { value: false },
      tenderBreakdownParsing: { value: false },
    }
    const { handleOpenTenderBreakdown } = useProjectDetailTaskActions({
      route: { params: { id: 12 } },
      userStore: { userName: '小王' },
      projectStore: {},
      projectsApi: { getLatestTenderBreakdown },
      isApiProject: { value: true },
      message: { success, error: vi.fn(), warning: vi.fn() },
      state,
      workflow: {},
    })

    await handleOpenTenderBreakdown()

    expect(getLatestTenderBreakdown).toHaveBeenCalledWith(12)
    expect(state.tenderBreakdownDialogVisible.value).toBe(false)
    expect(success).toHaveBeenCalledWith('已复用已解析的招标文件「招标文件.docx」，可直接拆解任务或生成标书初稿')
  })

  it('API 项目没有解析快照时点击解析入口才打开上传弹窗', async () => {
    const getLatestTenderBreakdown = vi.fn().mockResolvedValue({
      success: true,
      data: null,
    })
    const state = {
      project: { value: { id: 12, tasks: [] } },
      activities: { value: [] },
      tenderBreakdownDialogVisible: { value: false },
      tenderBreakdownParsing: { value: false },
    }
    const { handleOpenTenderBreakdown } = useProjectDetailTaskActions({
      route: { params: { id: 12 } },
      userStore: { userName: '小王' },
      projectStore: {},
      projectsApi: { getLatestTenderBreakdown },
      isApiProject: { value: true },
      message: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
      state,
      workflow: {},
    })

    await handleOpenTenderBreakdown()

    expect(getLatestTenderBreakdown).toHaveBeenCalledWith(12)
    expect(state.tenderBreakdownDialogVisible.value).toBe(true)
  })

  it('API 项目无解析快照但已有上传标书时点击解析入口直接复用上传文件', async () => {
    const success = vi.fn()
    const projectsApi = {
      getLatestTenderBreakdown: vi.fn().mockResolvedValue({
        success: true,
        data: null,
      }),
      getTenderBreakdownReadiness: vi.fn().mockResolvedValue({
        success: true,
        data: { ready: true },
      }),
      parseUploadedTenderBreakdown: vi.fn().mockResolvedValue({
        success: true,
        data: {
          document: {
            name: '已上传招标文件.docx',
            snapshotId: 701,
          },
        },
      }),
    }
    const state = {
      project: { value: { id: 12, tasks: [] } },
      activities: { value: [] },
      tenderBreakdownDialogVisible: { value: false },
      tenderBreakdownParsing: { value: false },
    }
    const { handleOpenTenderBreakdown } = useProjectDetailTaskActions({
      route: { params: { id: 12 } },
      userStore: { userName: '小王' },
      projectStore: {},
      projectsApi,
      isApiProject: { value: true },
      message: { success, error: vi.fn(), warning: vi.fn() },
      state,
      workflow: {},
    })

    await handleOpenTenderBreakdown()

    expect(projectsApi.getLatestTenderBreakdown).toHaveBeenCalledWith(12)
    expect(projectsApi.getTenderBreakdownReadiness).toHaveBeenCalledWith(12)
    expect(projectsApi.parseUploadedTenderBreakdown).toHaveBeenCalledWith(12)
    expect(state.tenderBreakdownDialogVisible.value).toBe(false)
    expect(success).toHaveBeenCalledWith('已复用项目已上传的招标文件「已上传招标文件.docx」，可直接拆解任务或生成标书初稿')
  })

  it('最新解析快照接口暂不可用时不展示 404 错误并回退到上传弹窗', async () => {
    const error = vi.fn()
    const getLatestTenderBreakdown = vi.fn().mockRejectedValue({
      response: {
        status: 404,
        data: {
          message: 'No static resource api/projects/12/tender-breakdown/latest.',
        },
      },
    })
    const state = {
      project: { value: { id: 12, tasks: [] } },
      activities: { value: [] },
      tenderBreakdownDialogVisible: { value: false },
      tenderBreakdownParsing: { value: false },
    }
    const { handleOpenTenderBreakdown } = useProjectDetailTaskActions({
      route: { params: { id: 12 } },
      userStore: { userName: '小王' },
      projectStore: {},
      projectsApi: { getLatestTenderBreakdown },
      isApiProject: { value: true },
      message: { success: vi.fn(), error, warning: vi.fn() },
      state,
      workflow: {},
    })

    await handleOpenTenderBreakdown()

    expect(getLatestTenderBreakdown).toHaveBeenCalledWith(12)
    expect(state.tenderBreakdownDialogVisible.value).toBe(true)
    expect(error).not.toHaveBeenCalled()
  })

  it('API 项目缺少 DeepSeek 配置时上传前提示配置指引且不解析文件', async () => {
    const file = new File(['招标正文'], '招标文件.docx')
    const warning = vi.fn()
    const projectsApi = {
      getTenderBreakdownReadiness: vi.fn().mockResolvedValue({
        success: true,
        data: {
          ready: false,
          message: 'DeepSeek API Key 未配置。请管理员到系统设置 → AI 模型配置中填写 DeepSeek provider key，或在服务端设置 DEEPSEEK_API_KEY 后重启。',
          settingsPath: '/settings',
        },
      }),
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
        tenderBreakdownParsing: { value: false },
      },
      workflow: {},
    })

    const result = await handleTenderBreakdownUpload(file)

    expect(result).toBe(false)
    expect(projectsApi.getTenderBreakdownReadiness).toHaveBeenCalledWith(12)
    expect(projectsApi.parseTenderBreakdown).not.toHaveBeenCalled()
    expect(warning).toHaveBeenCalledWith('DeepSeek API Key 未配置。请管理员到系统设置 → AI 模型配置中填写 DeepSeek provider key，或在服务端设置 DEEPSEEK_API_KEY 后重启。')
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

  it('API 项目切换任务状态为已取消时向后端传递 CANCELLED 枚举', async () => {
    const success = vi.fn()
    const error = vi.fn()
    const updateTaskStatus = vi.fn().mockResolvedValue({
      success: true,
      data: { id: 42, name: '资格审查', status: 'cancelled' },
    })
    const state = {
      project: ref({ id: 12, name: '测试项目', tasks: [{ id: 42, name: '资格审查', status: 'todo' }] }),
      activities: ref([]),
      scoreDraftDialogVisible: ref(false),
      currentTask: ref(null),
      taskDialogVisible: ref(false),
    }

    const { handleTaskStatusChange } = useProjectDetailTaskActions({
      route: { params: { id: '12' } },
      userStore: { userName: '测试用户', currentUser: { id: 9 } },
      projectStore: {},
      projectsApi: { updateTaskStatus },
      isApiProject: ref(true),
      message: { success, error, warning: vi.fn() },
      state,
      workflow: {},
    })

    await handleTaskStatusChange(state.project.value.tasks[0], 'cancelled')

    expect(updateTaskStatus).toHaveBeenCalledWith('12', 42, 'CANCELLED')
    expect(error).not.toHaveBeenCalled()
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
