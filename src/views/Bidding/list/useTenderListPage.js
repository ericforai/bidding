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
  let parseTimer = null

  const userRole = computed(() => resolveUserRole(userStore))
  const permissions = computed(() => buildPermissionFlags(userRole.value))
  const canManageTenders = computed(() => permissions.value.canManageTenders)
  const canCreateTender = computed(() => permissions.value.canCreateTender)
  const canDeleteTenders = computed(() => permissions.value.canDeleteTenders)
  const canSyncExternalSource = computed(() => permissions.value.canSyncExternalSource)
  const customerOpportunityCenterEnabled = computed(() => true)
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
    pending: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.PENDING)).length,
    tracking: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.TRACKING)).length,
    bidded: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.BIDDED)).length,
    abandoned: tenders.value.filter((tender) => matchesTenderStatus(tender.status, TENDER_STATUSES.ABANDONED)).length,
  }))

  const refreshTenderList = async () => {
    await biddingStore.getTenders({
      keyword: searchForm.value.keyword || undefined,
      region: searchForm.value.region || undefined,
      industry: searchForm.value.industry || undefined,
      status: searchForm.value.status || undefined,
      source: searchForm.value.source || undefined,
    })
  }

  const selection = useTenderSelection({ displayTenders })
  const sourceConfig = useTenderSourceConfig({
    refreshTenderList,
    searchForm,
    canSyncExternalSource,
  })
  const manualCreate = useManualTenderCreate({ tendersApi, refreshTenderList, canCreateTender })
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
      industry: searchForm.value.industry || undefined,
      status: searchForm.value.status || undefined,
      source: searchForm.value.source || undefined,
    }, '标讯列表导出成功')
  }

  const handleViewDetail = (id) => router.push(`/bidding/${id}`)

  const handleParticipate = (id) => {
    router.push({ path: '/project/create', query: { tenderId: id } })
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
    canManageTenders,
    canCreateTender,
    canDeleteTenders,
    canSyncExternalSource,
    customerOpportunityCenterEnabled,
    showTenderAiEntry,
    showParsingDialog,
    parseProgress,
    selection,
    sourceConfig,
    manualCreate,
    marketInsight,
    batchActions,
    distribution,
    handleSearch,
    handleReset,
    handleExport,
    handleViewDetail,
    handleParticipate,
    handleViewAllRecommend,
    handleOpenCustomerOpportunityCenter,
    openManualAdd,
    openSourceConfig,
    handleAIAnalysis,
  }
}
