// Input: httpClient for workflow-form requests and attachment files
// Output: workflowFormApi - dynamic workflow form schema, attachment upload and submission accessors
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

async function listAdminTemplates() {
  return httpClient.get('/api/admin/workflow-forms/templates')
}

async function listTemplateVersions(templateCode) {
  return httpClient.get(`/api/admin/workflow-forms/templates/${templateCode}/versions`)
}

async function listBusinessTypes() {
  return httpClient.get('/api/admin/workflow-forms/business-types')
}

async function createTemplateDraft(payload = {}) {
  return httpClient.post('/api/admin/workflow-forms/templates', payload)
}

async function updateTemplateDraft(templateCode, payload = {}) {
  return httpClient.put(`/api/admin/workflow-forms/templates/${templateCode}/draft`, payload)
}

async function saveOaBinding(templateCode, payload = {}) {
  return httpClient.put(`/api/admin/workflow-forms/templates/${templateCode}/oa-binding`, payload)
}

async function publishTemplate(templateCode) {
  return httpClient.post(`/api/admin/workflow-forms/templates/${templateCode}/publish`)
}

async function rollbackTemplateVersion(templateCode, version) {
  return httpClient.post(`/api/admin/workflow-forms/templates/${templateCode}/versions/${version}/rollback`)
}

async function testSubmitTemplate(templateCode, payload = {}) {
  return httpClient.post(`/api/admin/workflow-forms/templates/${templateCode}/oa/test-submit`, payload)
}

async function uploadWorkflowFormAttachment(templateCode, fieldKey, file, options = {}) {
  const formData = new FormData()
  formData.append('templateCode', normalizeWorkflowType(templateCode))
  formData.append('fieldKey', fieldKey)
  if (options.projectId !== undefined && options.projectId !== null && options.projectId !== '') {
    formData.append('projectId', String(options.projectId))
  }
  formData.append('file', file)
  return httpClient.post('/api/workflow-forms/attachments', formData)
}

export const workflowFormApi = {
  getFormDefinition,
  submitWorkflowForm,
  getWorkflowInstance,
  uploadWorkflowFormAttachment,
  listAdminTemplates,
  listTemplateVersions,
  listBusinessTypes,
  createTemplateDraft,
  updateTemplateDraft,
  saveOaBinding,
  publishTemplate,
  rollbackTemplateVersion,
  testSubmitTemplate
}

export default workflowFormApi
