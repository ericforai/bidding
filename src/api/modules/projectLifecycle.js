// Input: httpClient and 6-stage tender lifecycle request payloads
// Output: projectLifecycleApi - thin wrappers for /api/projects/{id}/{initiation,drafting,evaluation,result,retrospective,closure,stage}
// Pos: src/api/modules/ - Frontend API module layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import httpClient from '../client.js'

const base = (id) => `/api/projects/${id}`

export const projectLifecycleApi = {
  // WS-G stage snapshot
  getStage(id) {
    return httpClient.get(`${base(id)}/stage`)
  },

  // WS-A initiation
  getInitiation(id) {
    return httpClient.get(`${base(id)}/initiation`)
  },
  submitInitiation(id, payload) {
    return httpClient.post(`${base(id)}/initiation`, payload)
  },
  updateInitiation(id, payload) {
    return httpClient.patch(`${base(id)}/initiation`, payload)
  },

  // WS-B drafting
  getDrafting(id) {
    return httpClient.get(`${base(id)}/drafting`)
  },
  assignDraftingLeads(id, payload) {
    return httpClient.patch(`${base(id)}/drafting/leads`, payload)
  },
  advanceDrafting(id, payload = {}) {
    return httpClient.post(`${base(id)}/drafting/advance`, payload)
  },

  // WS-C evaluation
  getEvaluation(id) {
    return httpClient.get(`${base(id)}/evaluation`)
  },
  transitionEvaluationSubStage(id, payload) {
    return httpClient.patch(`${base(id)}/evaluation/sub-stage`, payload)
  },
  attachEvaluationEvidence(id, payload) {
    return httpClient.post(`${base(id)}/evaluation/evidence`, payload)
  },
  updateEvaluationForm(id, payload) {
    return httpClient.patch(`${base(id)}/evaluation/form`, payload)
  },
  abandonBid(id, payload) {
    return httpClient.post(`${base(id)}/evaluation/abandon`, payload)
  },
  submitToBid(id) {
    return httpClient.post(`${base(id)}/submit-to-bid-document`)
  },

  // WS-D result
  getResult(id) {
    return httpClient.get(`${base(id)}/result`)
  },
  registerResult(id, payload) {
    return httpClient.post(`${base(id)}/result`, payload)
  },

  // WS-E retrospective
  getRetrospective(id) {
    return httpClient.get(`${base(id)}/retrospective`)
  },
  submitRetrospective(id, payload) {
    return httpClient.post(`${base(id)}/retrospective`, payload)
  },
  reviewRetrospective(id, payload) {
    return httpClient.patch(`${base(id)}/retrospective/review`, payload)
  },

  // WS-F closure
  getClosurePreview(id) {
    return httpClient.get(`${base(id)}/closure/preview`)
  },
  submitClosure(id, payload) {
    return httpClient.post(`${base(id)}/closure`, payload)
  },
}

export default projectLifecycleApi
