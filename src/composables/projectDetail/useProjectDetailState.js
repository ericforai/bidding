import { computed, ref } from 'vue'
import { getAccessToken } from '@/api/session.js'
import { getDemoProjectById } from '@/api/mock-adapters/frontendDemo.js'
import { initialActivities } from './constants.js'

export function useProjectDetailState(context) {
  const { route, userStore, projectStore, isDemoMode, isApiProject } = context

  const loading = ref(true)
  const approvalHistory = ref([])
  const assetCheckResult = ref(null)

  const taskDialogVisible = ref(false)
  const resultDialogVisible = ref(false)
  const competitorDialogVisible = ref(false)
  const processDialogVisible = ref(false)
  const reviewerDialogVisible = ref(false)
  const scoreDraftDialogVisible = ref(false)
  const approvalDialogVisible = ref(false)
  const currentTask = ref(null)
  const currentApprovalItem = ref({})
  const approvalMode = ref('submit')
  const approvalType = ref({ type: 'project_review', typeName: '立项审批' })

  const showCompetitionIntel = ref(false)
  const showComplianceCheck = ref(false)
  const showVersionControl = ref(false)
  const showCollaboration = ref(false)
  const showROIAnalysis = ref(false)
  const showAutoTasks = ref(false)
  const showMobileCard = ref(false)
  const assistantPanelVisible = ref(false)

  const noticeFileList = ref([])
  const uploadAction = ref(isDemoMode ? '/api/upload' : '')
  const uploadHeaders = computed(() => {
    const token = getAccessToken()
    return token ? { Authorization: `Bearer ${token}` } : {}
  })

  const resultForm = ref({
    result: '',
    amount: null,
    contractPeriod: null,
    skuCount: '',
    noticeFile: '',
    competitors: [],
    techHighlights: '',
    priceStrategy: '',
    customerFeedback: '',
    improvements: '',
  })

  const competitorForm = ref({
    name: '',
    skuCount: '',
    category: '',
    discount: '',
    payment: '',
  })

  const activities = ref([...initialActivities])

  const project = computed(() => {
    if (projectStore.currentProject) return projectStore.currentProject
    if (!isDemoMode) return null
    return getDemoProjectById(String(route.params.id || ''))
  })

  const dialogProjectId = computed(() => project.value?.id ?? route.params.id)
  const canManageProjectTasks = computed(() => isDemoMode || isApiProject.value)
  const canManageProjectDocuments = computed(() => isDemoMode || isApiProject.value)
  const canSetProjectReminder = computed(() => isDemoMode || isApiProject.value)
  const currentApproval = computed(() => approvalHistory.value[0] || null)
  const currentUserRole = computed(() => userStore.currentUser?.role || '')
  const canApproveCurrent = computed(() => {
    const currentName = userStore.userName || userStore.currentUser?.name || ''
    return currentApproval.value?.currentApproverName === currentName || currentUserRole.value === 'admin'
  })

  return {
    loading,
    approvalHistory,
    assetCheckResult,
    taskDialogVisible,
    resultDialogVisible,
    competitorDialogVisible,
    processDialogVisible,
    reviewerDialogVisible,
    scoreDraftDialogVisible,
    approvalDialogVisible,
    currentTask,
    currentApprovalItem,
    approvalMode,
    approvalType,
    showCompetitionIntel,
    showComplianceCheck,
    showVersionControl,
    showCollaboration,
    showROIAnalysis,
    showAutoTasks,
    showMobileCard,
    assistantPanelVisible,
    noticeFileList,
    uploadAction,
    uploadHeaders,
    resultForm,
    competitorForm,
    activities,
    project,
    dialogProjectId,
    canManageProjectTasks,
    canManageProjectDocuments,
    canSetProjectReminder,
    currentApproval,
    currentUserRole,
    canApproveCurrent,
  }
}
