/**
 * 审批模块 API
 * 支持双模式切换
 */
import httpClient from '../client.js'
import { mockData } from '../mock.js'
import { isMockMode } from '../config.js'

// 初始化审批相关 Mock 数据
if (!mockData.approvalRequests) {
  mockData.approvalRequests = [
    {
      id: 'AR001',
      projectId: 'P001',
      projectName: '某央企智慧办公平台采购',
      type: 'project_review', // project_review, budget, seal, document
      typeId: 'project_review',
      typeName: '立项审批',
      applicantId: 'U001',
      applicantName: '小王',
      applicantDept: '华南销售部',
      status: 'pending', // pending, approved, rejected, cancelled
      currentApproverId: 'U003',
      currentApproverName: '李总',
      currentApproverRole: 'admin',
      comment: '',
      submitTime: '2025-02-26 10:30',
      approvalNodes: [
        {
          nodeId: 'N001',
          nodeName: '部门经理审批',
          approverId: 'U002',
          approverName: '张经理',
          approverRole: 'manager',
          status: 'approved',
          opinion: '同意立项',
          approvalTime: '2025-02-26 11:00',
          order: 1
        },
        {
          nodeId: 'N002',
          nodeName: '总经理审批',
          approverId: 'U003',
          approverName: '李总',
          approverRole: 'admin',
          status: 'pending',
          opinion: '',
          approvalTime: null,
          order: 2
        }
      ],
      attachments: [],
      formJson: {
        budget: 500,
        reason: '客户关系良好，历史有合作经验',
        riskLevel: 'medium'
      }
    },
    {
      id: 'AR002',
      projectId: 'P002',
      projectName: '华南电力集团集采项目',
      type: 'budget',
      typeId: 'budget',
      typeName: '预算审批',
      applicantId: 'U001',
      applicantName: '小王',
      applicantDept: '华南销售部',
      status: 'approved',
      currentApproverId: null,
      currentApproverName: null,
      currentApproverRole: null,
      comment: '预算合理，同意执行',
      submitTime: '2025-02-25 14:00',
      approvalNodes: [
        {
          nodeId: 'N003',
          nodeName: '部门经理审批',
          approverId: 'U002',
          approverName: '张经理',
          approverRole: 'manager',
          status: 'approved',
          opinion: '同意',
          approvalTime: '2025-02-25 14:30',
          order: 1
        }
      ],
      attachments: [],
      formJson: {
        amount: 25,
        items: '保证金20万，标书费5万'
      }
    },
    {
      id: 'AR003',
      projectId: 'P003',
      projectName: '深圳地铁自动化系统',
      type: 'seal',
      typeId: 'seal',
      typeName: '用印申请',
      applicantId: 'U002',
      applicantName: '张经理',
      applicantDept: '投标管理部',
      status: 'pending',
      currentApproverId: 'U003',
      currentApproverName: '李总',
      currentApproverRole: 'admin',
      comment: '',
      submitTime: '2025-02-26 09:00',
      approvalNodes: [
        {
          nodeId: 'N004',
          nodeName: '总经理审批',
          approverId: 'U003',
          approverName: '李总',
          approverRole: 'admin',
          status: 'pending',
          opinion: '',
          approvalTime: null,
          order: 1
        }
      ],
      attachments: [],
      formJson: {
        sealType: '公章',
        documentCount: 3,
        reason: '投标文件用印'
      }
    }
  ]
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

export const approvalApi = {
  /**
   * 提交审批申请
   */
  async submitApproval(projectId, type, formData = {}) {
    if (isMockMode()) {
      const project = mockData.projects.find(p => p.id === projectId)
      const newRequest = {
        id: 'AR' + Date.now(),
        projectId,
        projectName: project?.name || '未知项目',
        type: type.type || 'project_review',
        typeId: type.typeId || 'project_review',
        typeName: type.typeName || '立项审批',
        applicantId: formData.applicantId || 'U001',
        applicantName: formData.applicantName || '小王',
        applicantDept: formData.applicantDept || '投标管理部',
        status: 'pending',
        currentApproverId: formData.currentApproverId || 'U003',
        currentApproverName: formData.currentApproverName || '李总',
        currentApproverRole: formData.currentApproverRole || 'admin',
        comment: '',
        submitTime: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
        approvalNodes: formData.approvalNodes || [
          {
            nodeId: 'N' + Date.now(),
            nodeName: '审批',
            approverId: formData.currentApproverId || 'U003',
            approverName: formData.currentApproverName || '李总',
            approverRole: formData.currentApproverRole || 'admin',
            status: 'pending',
            opinion: '',
            approvalTime: null,
            order: 1
          }
        ],
        attachments: formData.attachments || [],
        formJson: formData.formJson || {}
      }
      mockData.approvalRequests.unshift(newRequest)
      return Promise.resolve({ success: true, data: newRequest })
    }

    return httpClient.post('/api/approvals', {
      projectId,
      type: type.type || type,
      ...formData
    })
  },

  /**
   * 获取待审批列表
   */
  async getPendingApprovals(params = {}) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const pending = mockData.approvalRequests.filter(r => r.status === 'pending')
          resolve({ success: true, data: clone(pending), total: pending.length })
        }, 200)
      })
    }

    return httpClient.get('/api/approvals/pending', { params })
  },

  /**
   * 获取我的审批申请列表
   */
  async getMyApplications(params = {}) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const myApps = mockData.approvalRequests.filter(r => r.applicantId === params.applicantId || 'U001')
          resolve({ success: true, data: clone(myApps), total: myApps.length })
        }, 200)
      })
    }

    return httpClient.get('/api/approvals/my', { params })
  },

  /**
   * 获取审批详情
   */
  async getApprovalDetail(requestId) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const request = mockData.approvalRequests.find(r => r.id === requestId)
          resolve({ success: true, data: clone(request) })
        }, 100)
      })
    }

    return httpClient.get(`/api/approvals/${requestId}`)
  },

  /**
   * 通过审批
   */
  async approveApproval(requestId, comment, formData = {}) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const request = mockData.approvalRequests.find(r => r.id === requestId)
          if (request) {
            const currentNode = request.approvalNodes.find(n => n.status === 'pending')
            if (currentNode) {
              currentNode.status = 'approved'
              currentNode.opinion = comment
              currentNode.approvalTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
            }

            // 检查是否所有节点都已通过
            const allApproved = request.approvalNodes.every(n => n.status === 'approved')
            if (allApproved) {
              request.status = 'approved'
              request.comment = comment
              // 更新项目状态
              const project = mockData.projects.find(p => p.id === request.projectId)
              if (project && project.status === 'reviewing') {
                project.status = 'bidding'
              }
            } else {
              // 移动到下一节点
              const nextNode = request.approvalNodes.find(n => n.status === 'pending')
              if (nextNode) {
                request.currentApproverId = nextNode.approverId
                request.currentApproverName = nextNode.approverName
                request.currentApproverRole = nextNode.approverRole
              }
            }
          }
          resolve({ success: true, data: clone(request) })
        }, 300)
      })
    }

    return httpClient.post(`/api/approvals/${requestId}/approve`, { comment, ...formData })
  },

  /**
   * 驳回审批
   */
  async rejectApproval(requestId, reason, formData = {}) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const request = mockData.approvalRequests.find(r => r.id === requestId)
          if (request) {
            const currentNode = request.approvalNodes.find(n => n.status === 'pending')
            if (currentNode) {
              currentNode.status = 'rejected'
              currentNode.opinion = reason
              currentNode.approvalTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
            }
            request.status = 'rejected'
            request.comment = reason
            // 更新项目状态
            const project = mockData.projects.find(p => p.id === request.projectId)
            if (project && project.status === 'reviewing') {
              project.status = 'draft'
            }
          }
          resolve({ success: true, data: clone(request) })
        }, 300)
      })
    }

    return httpClient.post(`/api/approvals/${requestId}/reject`, { reason, ...formData })
  },

  /**
   * 取消审批申请
   */
  async cancelApproval(requestId, reason = '') {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const request = mockData.approvalRequests.find(r => r.id === requestId)
          if (request && request.status === 'pending') {
            request.status = 'cancelled'
            request.comment = reason
          }
          resolve({ success: true })
        }, 200)
      })
    }

    return httpClient.post(`/api/approvals/${requestId}/cancel`, { reason })
  },

  /**
   * 获取审批统计数据
   */
  async getApprovalStatistics() {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const stats = {
            pendingCount: mockData.approvalRequests.filter(r => r.status === 'pending').length,
            approvedCount: mockData.approvalRequests.filter(r => r.status === 'approved').length,
            rejectedCount: mockData.approvalRequests.filter(r => r.status === 'rejected').length,
            todayCount: mockData.approvalRequests.filter(r => {
              const today = new Date().toLocaleDateString('zh-CN')
              return r.submitTime.includes(today)
            }).length,
            myPendingCount: mockData.approvalRequests.filter(r =>
              r.status === 'pending' && r.applicantId === 'U001'
            ).length
          }
          resolve({ success: true, data: stats })
        }, 200)
      })
    }

    return httpClient.get('/api/approvals/statistics')
  },

  /**
   * 获取项目关联的审批记录
   */
  async getProjectApprovals(projectId) {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const approvals = mockData.approvalRequests.filter(r => r.projectId === projectId)
          resolve({ success: true, data: clone(approvals) })
        }, 100)
      })
    }

    return httpClient.get(`/api/projects/${projectId}/approvals`)
  },

  /**
   * 撤回审批（重新提交）
   */
  async withdrawApproval(requestId, reason = '') {
    if (isMockMode()) {
      return new Promise((resolve) => {
        setTimeout(() => {
          const request = mockData.approvalRequests.find(r => r.id === requestId)
          if (request && request.status === 'pending') {
            // 重置所有已通过的节点
            request.approvalNodes.forEach(node => {
              if (node.status === 'approved') {
                node.status = 'pending'
                node.opinion = ''
                node.approvalTime = null
              }
            })
            request.status = 'pending'
            request.comment = reason
            // 回到第一个节点
            const firstNode = request.approvalNodes[0]
            if (firstNode) {
              request.currentApproverId = firstNode.approverId
              request.currentApproverName = firstNode.approverName
              request.currentApproverRole = firstNode.approverRole
            }
          }
          resolve({ success: true })
        }, 200)
      })
    }

    return httpClient.post(`/api/approvals/${requestId}/withdraw`, { reason })
  }
}

export default approvalApi
