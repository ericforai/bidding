// Input: httpClient and API mode switch for approval-related requests
// Output: approvalApi - approval workflow accessors for frontend consumers
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'
import { isMockMode } from '../config.js'

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

function formatRelativeTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)

  const diffMs = Date.now() - date.getTime()
  const minutes = Math.floor(diffMs / (1000 * 60))
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`

  const days = Math.floor(hours / 24)
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`

  return date.toLocaleDateString('zh-CN')
}

function statusLabel(status) {
  const map = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    CANCELLED: '已取消',
  }
  return map[String(status || '').toUpperCase()] || (status || '未知状态')
}

function normalizeApproval(item = {}) {
  return {
    id: item.id,
    projectId: item.projectId,
    projectName: item.projectName || `项目#${item.projectId ?? '--'}`,
    title: item.title || `${item.projectName || '项目'} - ${item.approvalType || '审批'}`,
    approvalType: item.approvalType || 'project_review',
    typeName: item.approvalType || '项目审批',
    status: String(item.status || 'PENDING').toUpperCase(),
    statusDescription: item.statusDescription || statusLabel(item.status),
    requesterName: item.requesterName || item.applicantName || '未知申请人',
    applicantName: item.requesterName || item.applicantName || '未知申请人',
    applicantDept: item.applicantDept || '投标管理部',
    currentApproverName: item.currentApproverName || '待分配',
    priority: Number(item.priority || 0),
    description: item.description || '暂无说明',
    submitTime: formatDateTime(item.submittedAt || item.submitTime || item.createdAt),
    submittedAt: item.submittedAt || item.submitTime || item.createdAt,
    dueDate: item.dueDate || '',
    isOverdue: Boolean(item.isOverdue),
    isNearDueDate: Boolean(item.isNearDueDate),
    processingHours: item.processingHours ?? null,
    approvalNodes: Array.isArray(item.actions)
      ? item.actions.map((action, index) => ({
          nodeName: action.actionType || `审批节点${index + 1}`,
          approverName: action.operatorName || action.operator || '系统',
          status: String(action.actionType || '').toLowerCase().includes('reject') ? 'rejected' : 'approved',
          comment: action.comment || '',
          actionTime: formatDateTime(action.createdAt || action.actionTime),
        }))
      : [],
    time: formatRelativeTime(item.submittedAt || item.submitTime || item.createdAt),
    raw: item,
  }
}

function buildMockPendingApprovals() {
  const now = Date.now()
  return [
    {
      id: 'mock-approval-1',
      projectId: 1,
      projectName: '某央企项目',
      title: '某央企项目 - 投标预算审批',
      approvalType: 'budget_review',
      status: 'PENDING',
      requesterName: '小王',
      applicantDept: '投标管理部',
      currentApproverName: '张经理',
      priority: 1,
      description: '申请追加投标预算与打印装订费用。',
      submittedAt: new Date(now - 2 * 60 * 60 * 1000).toISOString(),
      dueDate: new Date(now + 24 * 60 * 60 * 1000).toISOString(),
      isNearDueDate: true,
      actions: [],
    },
    {
      id: 'mock-approval-2',
      projectId: 2,
      projectName: '西部云项目',
      title: '西部云项目 - 立项申请',
      approvalType: 'project_review',
      status: 'PENDING',
      requesterName: '小王',
      applicantDept: '华南销售部',
      currentApproverName: '李总',
      priority: 2,
      description: '申请项目立项并启动内部资源协调。',
      submittedAt: new Date(now - 5 * 60 * 60 * 1000).toISOString(),
      dueDate: new Date(now + 12 * 60 * 60 * 1000).toISOString(),
      isNearDueDate: true,
      actions: [],
    },
  ].map(normalizeApproval)
}

function buildMockProjectApprovals(projectId) {
  return [
    {
      id: `mock-history-${projectId}-1`,
      projectId: Number(projectId),
      projectName: `项目#${projectId}`,
      title: '立项审批',
      approvalType: 'project_review',
      status: 'APPROVED',
      requesterName: '小王',
      applicantDept: '华南销售部',
      currentApproverName: '李总',
      priority: 1,
      description: '项目立项已完成审批，可进入编制阶段。',
      submittedAt: '2026-03-11T09:30:00',
      completedAt: '2026-03-11T11:00:00',
      actions: [
        {
          actionType: 'SUBMIT',
          operatorName: '小王',
          comment: '提交立项申请',
          createdAt: '2026-03-11T09:30:00',
        },
        {
          actionType: 'APPROVE',
          operatorName: '李总',
          comment: '同意立项，按计划推进。',
          createdAt: '2026-03-11T11:00:00',
        },
      ],
    },
  ].map(normalizeApproval)
}

async function getPendingApprovals(params = {}) {
  if (isMockMode()) {
    const data = buildMockPendingApprovals()
    return { success: true, data }
  }

  const response = await httpClient.get('/api/approvals/pending', { params })
  const rows = Array.isArray(response?.data?.content)
    ? response.data.content
    : Array.isArray(response?.data)
      ? response.data
      : []

  return {
    ...response,
    data: rows.map(normalizeApproval),
    page: response?.data?.pageable ?? null,
  }
}

async function getProjectApprovals(projectId, params = {}) {
  if (isMockMode()) {
    return { success: true, data: buildMockProjectApprovals(projectId) }
  }

  const response = await httpClient.get('/api/approvals/my', {
    params: { page: 0, size: 100, ...params },
  })

  const rows = Array.isArray(response?.data?.content)
    ? response.data.content
    : Array.isArray(response?.data)
      ? response.data
      : []

  return {
    ...response,
    data: rows
      .filter((item) => String(item.projectId) === String(projectId))
      .map(normalizeApproval),
  }
}

async function getMyApprovals(params = {}) {
  if (isMockMode()) {
    return { success: true, data: buildMockProjectApprovals(1) }
  }

  const response = await httpClient.get('/api/approvals/my', {
    params: { page: 0, size: 20, ...params },
  })
  const rows = Array.isArray(response?.data?.content)
    ? response.data.content
    : Array.isArray(response?.data)
      ? response.data
      : []

  return {
    ...response,
    data: rows.map(normalizeApproval),
    page: response?.data?.pageable ?? null,
  }
}

async function submitApproval(payload) {
  if (isMockMode()) {
    return {
      success: true,
      data: normalizeApproval({
        ...payload,
        id: `mock-submit-${Date.now()}`,
        status: 'PENDING',
        requesterName: payload.requesterName || '当前用户',
        currentApproverName: '张经理',
        submittedAt: new Date().toISOString(),
      }),
    }
  }

  const response = await httpClient.post('/api/approvals/submit', payload)
  return response
}

async function approve(id, payload) {
  if (isMockMode()) {
    return { success: true, data: { id, status: 'APPROVED', ...payload } }
  }
  return httpClient.post(`/api/approvals/${id}/approve`, payload)
}

async function reject(id, payload) {
  if (isMockMode()) {
    return { success: true, data: { id, status: 'REJECTED', ...payload } }
  }
  return httpClient.post(`/api/approvals/${id}/reject`, payload)
}

export const approvalApi = {
  getPendingApprovals,
  getProjectApprovals,
  getMyApprovals,
  submitApproval,
  approve,
  reject,
}

export default approvalApi
