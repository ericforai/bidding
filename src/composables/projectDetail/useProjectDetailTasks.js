import { taskTemplates } from './constants.js'

export function useProjectDetailTasks(context) {
  const { project, route, userStore, projectsApi, isApiProject, projectStore, deliverableTypeMap } = context

  const getTaskTemplateByProject = (currentProject) => {
    const industry = currentProject?.industry?.toLowerCase() || ''
    if (industry.includes('政府') || industry.includes('gov')) return taskTemplates.government
    if (industry.includes('能源') || industry.includes('电力') || industry.includes('energy')) return taskTemplates.energy
    if (industry.includes('交通') || industry.includes('地铁') || industry.includes('traffic')) return taskTemplates.traffic
    return taskTemplates.default
  }

  const handleGenerateTasks = () => {
    if (!project.value) return context.message.warning('项目信息未加载')
    if (isApiProject.value) {
      context.scoreDraftDialogVisible.value = true
      return
    }
    const template = getTaskTemplateByProject(project.value)
    const deadline = new Date(project.value.deadline)
    project.value.tasks = template.map((taskTemplate, index) => {
      const taskDeadline = new Date(deadline)
      taskDeadline.setDate(taskDeadline.getDate() - taskTemplate.deadlineOffset)
      return { id: `${project.value.id}_T${String(index + 1).padStart(3, '0')}`, name: taskTemplate.name, description: taskTemplate.description, owner: taskTemplate.owner, status: 'todo', priority: taskTemplate.priority, deadline: taskDeadline.toISOString().split('T')[0], hasDeliverable: taskTemplate.needsDeliverable, deliverableType: taskTemplate.deliverableType || 'other', deliverables: [] }
    })
    context.activities.value.unshift({ id: Date.now(), user: userStore.userName, action: `根据项目模板自动生成了 ${project.value.tasks.length} 个任务`, time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    context.message.success(`已自动生成 ${project.value.tasks.length} 个任务`)
  }

  const handleScoreDraftGenerated = (tasks) => {
    if (!project.value) return
    project.value.tasks = tasks.map((task) => ({ ...task, deliverables: Array.isArray(task.deliverables) ? task.deliverables : [], hasDeliverable: Boolean(task.hasDeliverable) }))
    context.activities.value.unshift({ id: Date.now(), user: userStore.userName, action: `根据评分标准生成了 ${tasks.length} 个正式任务`, time: new Date().toLocaleString('zh-CN', { hour12: false }) })
  }

  const handleAddTask = () => {
    if (!project.value) return
    const nextIndex = (project.value.tasks?.length || 0) + 1
    const dueDate = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000)
    const newTask = { name: `新增任务 ${nextIndex}`, owner: userStore.userName, assignee: userStore.userName, department: '投标管理部', dueDate: dueDate.toISOString().split('T')[0], priority: 'medium', status: 'todo', deliverables: [], hasDeliverable: false }
    if (!isApiProject.value) {
      if (!Array.isArray(project.value.tasks)) project.value.tasks = []
      project.value.tasks.unshift({ id: `TASK_${Date.now()}`, ...newTask })
      context.activities.value.unshift({ id: Date.now(), user: userStore.userName, action: `新增了任务「${newTask.name}」`, time: new Date().toLocaleString('zh-CN', { hour12: false }) })
      return context.message.success('已新增演示任务')
    }
    projectsApi.createTask(route.params.id, { title: newTask.name, description: '', assigneeId: userStore.currentUser?.id || null, assigneeName: userStore.userName, priority: 'MEDIUM', dueDate: dueDate.toISOString() }).then((result) => {
      if (!result?.success || !result?.data) throw new Error(result?.message || '新增任务失败')
      if (!Array.isArray(project.value.tasks)) project.value.tasks = []
      project.value.tasks.unshift({ ...result.data, deliverables: [], hasDeliverable: false })
      context.activities.value.unshift({ id: Date.now(), user: userStore.userName, action: `新增了任务「${result.data.name}」`, time: new Date().toLocaleString('zh-CN', { hour12: false }) })
      context.message.success('任务已新增')
    }).catch((error) => context.message.error(error.message || '新增任务失败'))
  }

  const handleResetTasks = () => {
    context.confirm('确认重置所有任务？这将清空当前项目的所有任务数据。', '重置确认', { confirmButtonText: '确认重置', cancelButtonText: '取消', type: 'warning' }).then(() => {
      if (project.value) {
        project.value.tasks = []
        context.activities.value.unshift({ id: Date.now(), user: userStore.userName, action: '重置了项目任务', time: new Date().toLocaleString('zh-CN', { hour12: false }) })
        context.message.success('任务已重置，可以重新拆解任务')
      }
    }).catch(() => {})
  }

  const handleTaskClick = (task) => { context.currentTask.value = task; context.taskDialogVisible.value = true }
  const handleTaskStatusChange = async (task, newStatus) => {
    if (task) {
      task.status = newStatus
      context.activities.value.unshift({ id: Date.now(), user: userStore.userName, action: `将任务"${task.name}"状态更新为${({ todo: '待办', doing: '进行中', review: '待审核', done: '已完成' }[newStatus] || newStatus)}`, time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    }
    if (!isApiProject.value) {
      await projectStore.updateTaskStatus(route.params.id, task?.id, newStatus)
      return context.message.success('任务状态已更新')
    }
    try {
      const result = await projectsApi.updateTaskStatus(route.params.id, task?.id, ({ todo: 'TODO', doing: 'IN_PROGRESS', done: 'COMPLETED', review: 'CANCELLED' }[newStatus] || 'TODO'))
      if (!result?.success || !result?.data) throw new Error(result?.message || '任务状态更新失败')
      Object.assign(task, result.data)
      context.message.success('任务状态已更新')
    } catch (error) {
      context.message.error(error.message || '任务状态更新失败')
    }
  }

  const handleAddDeliverable = (taskId, deliverable) => {
    const task = project.value?.tasks?.find((t) => t.id === taskId)
    if (!task) return
    if (!task.deliverables) task.deliverables = []
    task.deliverables.push(deliverable)
    task.hasDeliverable = true
    context.activities.value.unshift({ id: Date.now(), user: userStore.userName, action: `为任务"${task.name}"上传了交付物: ${deliverable.name}`, time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    context.message.success('交付物已添加')
  }
  const handleRemoveDeliverable = (taskId, deliverableId) => {
    const task = project.value?.tasks?.find((t) => t.id === taskId)
    if (!task?.deliverables) return
    const deliverable = task.deliverables.find((d) => d.id === deliverableId)
    task.deliverables = task.deliverables.filter((d) => d.id !== deliverableId)
    if (task.deliverables.length === 0) task.hasDeliverable = false
    context.activities.value.unshift({ id: Date.now(), user: userStore.userName, action: `删除了任务"${task.name}"的交付物: ${deliverable?.name}`, time: new Date().toLocaleString('zh-CN', { hour12: false }) })
    context.message.success('交付物已删除')
  }
  const handleSubmitToDocument = () => {
    if (!project.value?.tasks) return
    if (!project.value.tasks.every((t) => t.status === 'done')) return context.message.warning('请先完成所有任务后再提交至标书编写流程')
    const tasksWithDeliverables = project.value.tasks.filter((t) => t.deliverables && t.deliverables.length > 0)
    if (tasksWithDeliverables.length === 0) return context.message.warning('请至少上传一个任务的交付物后再提交')
    context.confirm(`所有任务已完成，确认提交至标书编写流程？\n\n已完成任务数: ${project.value.tasks.length}\n已上传交付物: ${tasksWithDeliverables.length} 个任务`, '提交确认', { confirmButtonText: '确认提交', cancelButtonText: '取消', type: 'success' }).then(() => {
      context.handleInitiateProcess()
      context.activities.value.unshift({ id: Date.now(), user: userStore.userName, action: '所有任务已完成，提交至标书编写流程', time: new Date().toLocaleString('zh-CN', { hour12: false }) })
      context.message.success('已提交至标书编写流程，可开始编制标书')
    }).catch(() => {})
  }

  return {
    deliverableTypeMap,
    handleGenerateTasks, handleScoreDraftGenerated, handleAddTask, handleResetTasks, handleTaskClick, handleTaskStatusChange,
    handleAddDeliverable, handleRemoveDeliverable, handleSubmitToDocument,
  }
}
