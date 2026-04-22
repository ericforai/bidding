// Input: Workbench API DTOs and support request forms
// Output: pure Workbench normalizers, mergers, validators, and payload builders
// Pos: src/views/Dashboard/ - Dashboard pure core helpers
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { formatTodoDeadline } from '@/views/Dashboard/workbench-formatters.js'

export function normalizeApiTodo(task) {
  const priority = String(task?.priority || 'MEDIUM').toLowerCase()
  return {
    id: task?.id,
    title: task?.title || '',
    priority,
    deadline: formatTodoDeadline(task?.dueDate),
    done: task?.status === 'COMPLETED',
    type: 'task',
    sourceType: 'task',
    rawStatus: task?.status,
  }
}

export function mergePriorityTodos(alertTodos = [], apiTodos = [], limit = 8) {
  return [...(alertTodos || []), ...(apiTodos || [])].slice(0, limit)
}

export function normalizePendingApproval(item) {
  return {
    ...item,
    title: item?.title || `${item?.projectName || ''} - ${item?.typeName || ''}`,
    type: item?.approvalType || 'project_review',
    department: item?.applicantDept || '投标管理部',
    time: item?.time || item?.submitTime || '',
  }
}

export function approvalStatusToProcessStatus(status) {
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'APPROVED') return 'in-progress'
  if (normalized === 'REJECTED' || normalized === 'CANCELLED') return 'urgent'
  return 'pending'
}

export function normalizeProcess(item) {
  const normalizedStatus = String(item?.status || '').toUpperCase()
  return {
    id: item?.id,
    title: item?.title || `${item?.projectName || ''} - ${item?.typeName || ''}`,
    status: approvalStatusToProcessStatus(item?.status),
    description: item?.description || '暂无说明',
    progress: normalizedStatus === 'APPROVED' ? 100 : normalizedStatus === 'PENDING' ? 55 : 0,
    time: item?.submittedAt || item?.submitTime || item?.time || '',
  }
}

export function normalizeSupportProject(item) {
  return { id: Number(item?.id), name: item?.name || item?.projectName || `项目#${item?.id}` }
}

export function normalizeSupportProjects(items) {
  return (Array.isArray(items) ? items : [])
    .map(normalizeSupportProject)
    .filter((item) => Number.isFinite(item.id))
}

export function createDefaultSupportRequestForm(projects = []) {
  return { projectId: projects[0]?.id || null, type: 'bid_support', dueDate: '', description: '' }
}

export function validateSupportRequest(form) {
  if (!form?.projectId) return { valid: false, message: '请选择关联项目' }
  if (!String(form?.description || '').trim()) return { valid: false, message: '请填写需求说明' }
  return { valid: true, message: '' }
}

export function buildSupportRequestPayload(form, projects = []) {
  const projectId = Number(form?.projectId)
  const selectedProject = projects.find((item) => item.id === projectId)
  const projectName = selectedProject?.name || `项目#${form?.projectId}`
  return {
    projectId,
    projectName,
    approvalType: form?.type || 'bid_support',
    title: `${selectedProject?.name || '当前项目'} - 标书支持申请`,
    description: String(form?.description || '').trim(),
    dueDate: form?.dueDate || null,
    priority: 1,
  }
}
