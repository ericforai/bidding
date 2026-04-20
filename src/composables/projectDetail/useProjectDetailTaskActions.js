import { ElMessageBox } from 'element-plus'
import { taskTemplates } from './constants.js'

export function useProjectDetailTaskActions(context) {
  const { route, userStore, projectStore, projectsApi, isApiProject, message, state, workflow } = context
  const pushActivity = (action) => state.activities.value.unshift({ id: Date.now(), user: userStore.userName, action, time: new Date().toLocaleString('zh-CN', { hour12: false }) })

  const getTaskTemplateByProject = (project) => {
    const industry = project?.industry?.toLowerCase() || ''
    if (industry.includes('政府') || industry.includes('gov')) return taskTemplates.government
    if (industry.includes('能源') || industry.includes('电力') || industry.includes('energy')) return taskTemplates.energy
    if (industry.includes('交通') || industry.includes('地铁') || industry.includes('traffic')) return taskTemplates.traffic
    return taskTemplates.default
  }

  const handleGenerateTasks = () => {
    if (!state.project.value) return message.warning('项目信息未加载')
    if (isApiProject.value) {
      state.scoreDraftDialogVisible.value = true
      return
    }
    const deadline = new Date(state.project.value.deadline)
    state.project.value.tasks = getTaskTemplateByProject(state.project.value).map((taskTemplate, index) => {
      const taskDeadline = new Date(deadline)
      taskDeadline.setDate(taskDeadline.getDate() - taskTemplate.deadlineOffset)
      return { id: `${state.project.value.id}_T${String(index + 1).padStart(3, '0')}`, name: taskTemplate.name, description: taskTemplate.description, owner: taskTemplate.owner, status: 'todo', priority: taskTemplate.priority, deadline: taskDeadline.toISOString().split('T')[0], hasDeliverable: taskTemplate.needsDeliverable, deliverableType: taskTemplate.deliverableType || 'other', deliverables: [] }
    })
    pushActivity(`根据项目模板自动生成了 ${state.project.value.tasks.length} 个任务`)
    message.success(`已自动生成 ${state.project.value.tasks.length} 个任务`)
  }

  const handleScoreDraftGenerated = (tasks) => {
    if (!state.project.value) return
    state.project.value.tasks = tasks.map((task) => ({ ...task, deliverables: Array.isArray(task.deliverables) ? task.deliverables : [], hasDeliverable: Boolean(task.hasDeliverable) }))
    pushActivity(`根据评分标准生成了 ${tasks.length} 个正式任务`)
  }

  const handleAddTask = () => {
    if (!state.project.value) return
    const nextIndex = (state.project.value.tasks?.length || 0) + 1
    const dueDate = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000)
    const newTask = { name: `新增任务 ${nextIndex}`, owner: userStore.userName, assignee: userStore.userName, department: '投标管理部', dueDate: dueDate.toISOString().split('T')[0], priority: 'medium', status: 'todo', deliverables: [], hasDeliverable: false }

    if (!isApiProject.value) {
      state.project.value.tasks = Array.isArray(state.project.value.tasks) ? state.project.value.tasks : []
      state.project.value.tasks.unshift({ id: `TASK_${Date.now()}`, ...newTask })
      pushActivity(`新增了任务「${newTask.name}」`)
      message.success('已新增演示任务')
      return
    }

    projectsApi.createTask(route.params.id, { title: newTask.name, description: '', assigneeId: userStore.currentUser?.id || null, assigneeName: userStore.userName, priority: 'MEDIUM', dueDate: dueDate.toISOString() }).then((result) => {
      if (!result?.success || !result?.data) throw new Error(result?.message || '新增任务失败')
      state.project.value.tasks.unshift({ ...result.data, deliverables: [], hasDeliverable: false })
      pushActivity(`新增了任务「${result.data.name}」`)
      message.success('任务已新增')
    }).catch((error) => message.error(error.message || '新增任务失败'))
  }

  const handleResetTasks = () => {
    ElMessageBox.confirm('确认重置所有任务？这将清空当前项目的所有任务数据。', '重置确认', { confirmButtonText: '确认重置', cancelButtonText: '取消', type: 'warning' }).then(() => {
      state.project.value.tasks = []
      pushActivity('重置了项目任务')
      message.success('任务已重置，可以重新拆解任务')
    }).catch(() => {})
  }

  const handleTaskClick = (task) => { state.currentTask.value = task; state.taskDialogVisible.value = true }
  const handleAddDeliverable = (taskId, deliverable) => {
    const task = state.project.value?.tasks?.find((item) => item.id === taskId)
    if (!task) return
    task.deliverables = task.deliverables || []
    task.deliverables.push(deliverable)
    task.hasDeliverable = true
    pushActivity(`为任务"${task.name}"上传了交付物: ${deliverable.name}`)
    message.success('交付物已添加')
  }
  const handleRemoveDeliverable = (taskId, deliverableId) => {
    const task = state.project.value?.tasks?.find((item) => item.id === taskId)
    if (!task?.deliverables) return
    const deliverable = task.deliverables.find((item) => item.id === deliverableId)
    task.deliverables = task.deliverables.filter((item) => item.id !== deliverableId)
    task.hasDeliverable = task.deliverables.length > 0
    pushActivity(`删除了任务"${task.name}"的交付物: ${deliverable?.name}`)
    message.success('交付物已删除')
  }

  const handleSubmitToDocument = async () => {
    const tasks = state.project.value?.tasks || []
    if (!tasks.length) return

    const allCompleted = tasks.every((task) => task.status === 'done')
    if (!allCompleted) {
      message.warning('请先完成所有任务后再提交至标书编写流程')
      return
    }

    const tasksWithDeliverables = tasks.filter((task) => Array.isArray(task.deliverables) && task.deliverables.length > 0)
    if (!tasksWithDeliverables.length) {
      message.warning('请至少上传一个任务的交付物后再提交')
      return
    }

    try {
      await ElMessageBox.confirm(
        `所有任务已完成，确认提交至标书编写流程？\n\n已完成任务数: ${tasks.length}\n已上传交付物: ${tasksWithDeliverables.length} 个任务`,
        '提交确认',
        {
          confirmButtonText: '确认提交',
          cancelButtonText: '取消',
          type: 'success',
        },
      )
      await workflow.handleInitiateProcess()
      pushActivity('所有任务已完成，提交至标书编写流程')
      message.success('已提交至标书编写流程，可开始编制标书')
    } catch {
      return
    }
  }

  return { handleGenerateTasks, handleScoreDraftGenerated, handleAddTask, handleResetTasks, handleTaskClick, handleAddDeliverable, handleRemoveDeliverable, handleSubmitToDocument }
}
