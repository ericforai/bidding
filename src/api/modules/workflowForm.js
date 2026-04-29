// Input: httpClient for workflow-form requests
// Output: workflowFormApi - dynamic workflow form schema and submission accessors
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'

function normalizeWorkflowType(workflowType) {
  return String(workflowType || 'QUALIFICATION_BORROW').toUpperCase()
}

async function getFormDefinition(workflowType = 'QUALIFICATION_BORROW') {
  const normalized = normalizeWorkflowType(workflowType)
  return httpClient.get(`/api/workflow-forms/templates/${normalized}/active`)
}

async function submitWorkflowForm(workflowType, payload = {}) {
  const normalized = normalizeWorkflowType(workflowType)
  return httpClient.post('/api/workflow-forms/instances', {
    templateCode: payload.templateCode || normalized,
    businessType: payload.businessType || normalized,
    projectId: payload.projectId ?? payload.formData?.projectId ?? null,
    applicantName: payload.applicantName || '',
    formData: payload.formData || {}
  })
}

async function getWorkflowInstance(id) {
  return httpClient.get(`/api/workflow-forms/instances/${id}`)
}

export const workflowFormApi = {
  getFormDefinition,
  submitWorkflowForm,
  getWorkflowInstance
}

export default workflowFormApi
