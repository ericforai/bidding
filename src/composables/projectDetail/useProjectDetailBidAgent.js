// Input: project detail route/router context and bid-agent API module
// Output: drawer state and lifecycle actions for project bid writing agent runs
// Pos: src/composables/projectDetail/ - Project Detail feature composables

import { computed, ref } from 'vue'
import { bidAgentApi as defaultBidAgentApi } from '@/api/modules/bidAgent.js'

function getResponseData(response) {
  return response?.data ?? null
}

function isFailedResponse(response) {
  return response?.success === false
}

function getMessage(error, fallback) {
  return error?.message || fallback
}

function resolveRunId(run) {
  return run?.runId ?? run?.id ?? null
}

export function useProjectDetailBidAgent(context) {
  const { route, router, project, message, bidAgentApi = defaultBidAgentApi } = context

  const drawerVisible = ref(false)
  const currentRun = ref(null)
  const applyResult = ref(null)
  const reviewResult = ref(null)
  const error = ref('')
  const creating = ref(false)
  const fetching = ref(false)
  const applying = ref(false)
  const reviewing = ref(false)

  const projectId = computed(() => project?.value?.id ?? route.params.id)
  const currentRunId = computed(() => resolveRunId(currentRun.value))
  const isBusy = computed(() => creating.value || fetching.value || applying.value || reviewing.value)

  const openDrawer = () => {
    drawerVisible.value = true
  }

  const reportError = (err, fallback) => {
    const text = getMessage(err, fallback)
    error.value = text
    message?.error?.(text)
  }

  const ensureRunId = () => {
    if (currentRunId.value) return currentRunId.value
    const text = '请先启动 AI 生成初稿任务'
    error.value = text
    message?.warning?.(text)
    return null
  }

  const createRun = async (payload = {}) => {
    drawerVisible.value = true
    creating.value = true
    error.value = ''
    applyResult.value = null
    reviewResult.value = null

    try {
      const response = await bidAgentApi.createRun(projectId.value, payload)
      if (isFailedResponse(response)) throw new Error(response.message || '启动 AI 生成初稿失败')
      currentRun.value = getResponseData(response)
      message?.success?.('AI 初稿生成任务已启动')
      return currentRun.value
    } catch (err) {
      reportError(err, '启动 AI 生成初稿失败')
      return null
    } finally {
      creating.value = false
    }
  }

  const fetchRun = async (runId = currentRunId.value) => {
    if (!runId) return null
    fetching.value = true
    error.value = ''

    try {
      const response = await bidAgentApi.getRun(projectId.value, runId)
      if (isFailedResponse(response)) throw new Error(response.message || '获取 AI 生成状态失败')
      currentRun.value = getResponseData(response)
      return currentRun.value
    } catch (err) {
      reportError(err, '获取 AI 生成状态失败')
      return null
    } finally {
      fetching.value = false
    }
  }

  const applyBidAgentResult = async (options = {}) => {
    const runId = ensureRunId()
    if (!runId) return null

    const { navigate = false, ...payload } = options
    applying.value = true
    error.value = ''

    try {
      const response = await bidAgentApi.applyRun(projectId.value, runId, payload)
      if (isFailedResponse(response)) throw new Error(response.message || '写入文档编辑器失败')
      applyResult.value = getResponseData(response) || response
      message?.success?.('AI 初稿已写入文档编辑器')
      if (navigate) goToEditor(applyResult.value)
      return applyResult.value
    } catch (err) {
      reportError(err, '写入文档编辑器失败')
      return null
    } finally {
      applying.value = false
    }
  }

  const createReview = async (payload = {}) => {
    const runId = ensureRunId()
    if (!runId) return null

    reviewing.value = true
    error.value = ''

    try {
      const response = await bidAgentApi.createReview(projectId.value, { runId, ...payload })
      if (isFailedResponse(response)) throw new Error(response.message || '发起 AI 初稿审查失败')
      reviewResult.value = getResponseData(response) || response
      message?.success?.('AI 初稿审查已发起')
      return reviewResult.value
    } catch (err) {
      reportError(err, '发起 AI 初稿审查失败')
      return null
    } finally {
      reviewing.value = false
    }
  }

  function goToEditor(target = applyResult.value) {
    const targetProjectId = target?.projectId ?? projectId.value
    const query = {
      bidAgentRunId: String(currentRunId.value || ''),
    }
    if (target?.documentId) query.documentId = String(target.documentId)
    if (target?.jobId) query.jobId = String(target.jobId)

    return router.push({
      name: 'DocumentEditor',
      params: { id: String(targetProjectId) },
      query,
    })
  }

  return {
    drawerVisible,
    currentRun,
    applyResult,
    reviewResult,
    error,
    creating,
    fetching,
    applying,
    reviewing,
    projectId,
    currentRunId,
    isBusy,
    openDrawer,
    createRun,
    fetchRun,
    applyBidAgentResult,
    createReview,
    goToEditor,
  }
}
