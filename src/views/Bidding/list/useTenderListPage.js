// Input: Vue router, stores, API modules, and browser viewport
// Output: composed state/actions for the bidding list page shell
// Pos: src/views/Bidding/list/ - Bidding list page composition root
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBiddingStore } from '@/stores/bidding'
import { useUserStore } from '@/stores/user'
import { tendersApi } from '@/api/modules/tenders'
import { batchTendersApi } from '@/api/modules/tenders/batch.js'
import { ExportType } from '@/api'
import { useExport } from '@/composables/useExport'
import {
  DEFAULT_SEARCH_FORM,
} from './constants.js'
import { buildPermissionFlags, resolveUserRole } from './helpers.js'
import {
  matchesTenderStatus,
  TENDER_STATUSES,
} from '../bidding-utils-status.js'
import { useManualTenderCreate } from './useManualTenderCreate.js'
import { useTenderBulkImport } from './useTenderBulkImport.js'
import { useMarketInsight } from './useMarketInsight.js'
import { useTenderBatchActions } from './useTenderBatchActions.js'
import { useTenderDistribution } from './useTenderDistribution.js'
import { useTenderSelection } from './useTenderSelection.js'
import { useTenderSourceConfig } from './useTenderSourceConfig.js'

export function useTenderListPage() {
  const router = useRouter()
  const biddingStore = useBiddingStore()
  const userStore = useUserStore()
  const searchForm = ref({ ...DEFAULT_SEARCH_FORM })
  const viewMode = ref('all')
  const isMobile = ref(false)
  const pagination = ref({ currentPage: 1, pageSize: 10 })
  const followedTenders = ref([])
  const showParsingDialog = ref(false)
  const parseProgress = ref(0)
  const parsingTenderId = ref(null)
  const evaluationDialogVisible = ref(false)
  const reviewDialogVisible = ref(false)
  const currentTenderId = ref(null)
  const currentTenderTitle = ref('')
  let parseTimer = null

  const userRole = computed(() => resolveUserRole(userStore))
  const isAdmin = computed(() => userRole.value === 'ADMIN')
  const permissions = computed(() => buildPermissionFlags(userRole.value))
  const canManageTenders = computed(() => permissions.value.canManageTenders)
  const canCreateTender = computed(() => permissions.value.canCreateTender)
  const canDeleteTenders = computed(() => permissions.value.canDeleteTenders)
  const canSyncExternalSource = computed(() => permissions.value.canSyncExternalSource)
  const customerOpportunityCenterEnabled = computed(() => false)
  const showTenderAiEntry = computed(() => true)
  const tenders = computed(() => biddingStore.tenders || [])

  const filteredTenders = computed(() => {
    if (viewMode.value === 'all') return [...tenders.value]
    return tenders.value.filter((tender) => matchesTenderStatus(tender.status, viewMode.value))
  })

  const filteredRecommendTenders = computed(() =>
    filteredTenders.value.filter((tender) => Number(tender.aiScore || 0) >= 85).slice(0, 3),
  )

  const displayTenders = computed(() => {
    const start = (pagination.value.currentPage - 1) * pagination.value.pageSize
    return filteredTenders.value.slice(start, start + pagination.value.pageSize)
  })

  const statusCounts = computed(() => ({
    all: tenders.value.length,
    pendingAssignment: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.PENDING_ASSIGNMENT)).length,
    tracking: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.TRACKING)).length,
    evaluated: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.EVALUATED)).length,
    bidding: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.BIDDING)).length,
    won: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.WON)).length,
    lost: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.LOST)).length,
    abandoned: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.ABANDONED)).length,
  }))

  const refreshTenderList = async () => {
    await biddingStore.getTenders({
      keyword: searchForm.value.keyword || undefined,
      region: searchForm.value.region || undefined,
      status: searchForm.value.status || undefined,
      sourceType: searchForm.value.sourceType || undefined,
      source: searchForm.value.source || undefined,
      customerType: searchForm.value.customerType || undefined,
      priority: searchForm.value.priority || undefined,
      registrationDeadlineFrom: searchForm.value.registrationDeadlineFrom || undefined,
      registrationDeadlineTo: searchForm.value.registrationDeadlineTo || undefined,
      bidOpeningTimeFrom: searchForm.value.bidOpeningTimeFrom || undefined,
      bidOpeningTimeTo: searchForm.value.bidOpeningTimeTo || undefined,
    })
  }

  const selection = useTenderSelection({ displayTenders })
  const sourceConfig = useTenderSourceConfig({
    refreshTenderList,
    searchForm,
    canSyncExternalSource,
  })
  const manualCreate = useManualTenderCreate({ tendersApi, refreshTenderList, canCreateTender })
  const bulkImport = useTenderBulkImport({ tendersApi, refreshTenderList, canCreateTender })
  const marketInsight = useMarketInsight()
  const batchActions = useTenderBatchActions({
    batchTendersApi,
    tendersApi,
    selectedTenders: selection.selectedTenders,
    followedTenders,
    clearSelection: selection.handleClearSelection,
    refreshTenderList,
    canManageTenders,
    canDeleteTenders,
    router,
  })
  const distribution = useTenderDistribution({
    batchTendersApi,
    selectedTenders: selection.selectedTenders,
    selectSingleTender: selection.selectSingleTender,
    clearSelection: selection.handleClearSelection,
    refreshTenderList,
    showBatchOperationFeedback: batchActions.showBatchOperationFeedback,
    canManageTenders,
  })

  const checkMobile = () => {
    isMobile.value = typeof window !== 'undefined' && window.innerWidth < 768
  }

  const handleSearch = async () => {
    pagination.value.currentPage = 1
    await refreshTenderList()
  }

  const handleReset = async () => {
    searchForm.value = { ...DEFAULT_SEARCH_FORM }
    pagination.value.currentPage = 1
    await refreshTenderList()
  }

  const handleExport = () => {
    const { exportExcel } = useExport()
    exportExcel(ExportType.TENDERS, {
      keyword: searchForm.value.keyword || undefined,
      region: searchForm.value.region || undefined,
      status: searchForm.value.status || undefined,
      sourceType: searchForm.value.sourceType || undefined,
    }, '标讯列表导出成功')
  }

  const handleViewDetail = (id) => router.push(`/bidding/${id}`)

  const handleParticipate = (id) => {
    router.push({ path: '/project/create', query: { tenderId: id } })
  }

  const handleEvaluate = (id) => {
    // Redirect to score analysis page with tenderId
    router.push({ path: '/analytics/score-analysis', query: { tenderId: id, action: 'create' } })
  }

  const handleViewAllRecommend = () => {
    searchForm.value = { ...DEFAULT_SEARCH_FORM }
    viewMode.value = 'all'
  }

  const handleOpenCustomerOpportunityCenter = () => {
    router.push('/bidding/customer-opportunities')
  }

  const openManualAdd = () => {
    if (!canCreateTender.value) return ElMessage.error('当前账号无权人工录入标讯')
    manualCreate.showManualAdd.value = true
  }

  const openSourceConfig = () => {
    if (!canSyncExternalSource.value) return ElMessage.error('当前账号无权配置标讯源')
    sourceConfig.showSourceConfig.value = true
  }

  const handleAIAnalysis = (id) => {
    if (!showTenderAiEntry.value) return
    parsingTenderId.value = id
    parseProgress.value = 0
    showParsingDialog.value = true
    clearInterval(parseTimer)
    parseTimer = setInterval(() => {
      parseProgress.value = Math.min(100, parseProgress.value + 20)
      if (parseProgress.value >= 100) {
        clearInterval(parseTimer)
        showParsingDialog.value = false
        router.push(`/bidding/ai-analysis/${id}`)
      }
    }, 250)
  }

  const handleEdit = (row) => {
    currentTenderId.value = row.id
    currentTenderTitle.value = row.title
    evaluationDialogVisible.value = true
  }

  const handleReview = (row) => {
    currentTenderId.value = row.id
    currentTenderTitle.value = row.title
    reviewDialogVisible.value = true
  }

  const handleEvaluationSuccess = () => {
    refreshTenderList()
  }

  const handleReviewSuccess = () => {
    refreshTenderList()
  }

  onMounted(async () => {
    checkMobile()
    window.addEventListener('resize', checkMobile)
    await refreshTenderList()
    sourceConfig.loadSavedConfig()
  })

  onUnmounted(() => {
    window.removeEventListener('resize', checkMobile)
    clearInterval(parseTimer)
  })

  return {
    searchForm,
    viewMode,
    isMobile,
    pagination,
    filteredTenders,
    filteredRecommendTenders,
    displayTenders,
    statusCounts,
    userRole,
    isAdmin,
    canManageTenders,
    canCreateTender,
    canDeleteTenders,
    canSyncExternalSource,
    customerOpportunityCenterEnabled,
    showTenderAiEntry,
    showParsingDialog,
    parseProgress,
    evaluationDialogVisible,
    reviewDialogVisible,
    currentTenderId,
    currentTenderTitle,
    selection,
    sourceConfig,
    manualCreate,
    bulkImport,
    marketInsight,
    batchActions,
    distribution,
    handleSearch,
    handleReset,
    handleExport,
    handleViewDetail,
    handleParticipate,
    handleEvaluate,
    handleViewAllRecommend,
    handleOpenCustomerOpportunityCenter,
    openManualAdd,
    openSourceConfig,
    handleAIAnalysis,
    handleEdit,
    handleReview,
    handleEvaluationSuccess,
    handleReviewSuccess,
  }
}
