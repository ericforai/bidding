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
