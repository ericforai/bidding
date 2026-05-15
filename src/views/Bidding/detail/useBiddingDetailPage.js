import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { bidMatchScoringApi, tendersApi } from '@/api'
import { useRoute } from 'vue-router'
import {
  formatTenderDisplayField, formatTenderIndustry, getTenderDateTimeParts,
} from '../bidding-utils.js'
import { getTenderStatusTagType, getTenderStatusText } from '../bidding-utils-status.js'
import { useMatchScoreState } from './useMatchScoreState.js'
import { useTenderActions } from './useTenderActions.js'

export function useBiddingDetailPage() {
  const route = useRoute()

  const tender = ref(null)
  const matchScore = ref(null)
  const scoreLoading = ref(false)
  const scoreError = ref('')

  const { matchScoreState, scoreEmptyText, scoreEmptyDescription } =
    useMatchScoreState(matchScore, scoreLoading, ref(false), scoreError)

  const regionMeta = computed(() => formatTenderDisplayField(tender.value?.region))
  const industryMeta = computed(() => formatTenderIndustry(tender.value?.industry))
  const deadlineParts = computed(() => getTenderDateTimeParts(tender.value?.deadline))

  const getScoreClass = (score) => score >= 90 ? 'score-excellent' : score >= 80 ? 'score-good' : 'score-normal'
  const getStatusType = (status) => getTenderStatusTagType(status)
  const getStatusText = (status) => getTenderStatusText(status)
  const getDeadlineClass = (deadline) => {
    const diffDays = Math.ceil((new Date(deadline) - new Date()) / (1000 * 60 * 60 * 24))
    if (diffDays <= 3) return 'deadline-urgent'
    if (diffDays <= 7) return 'deadline-warning'
    return ''
  }

  const loadMatchScore = async (tenderId) => {
    scoreLoading.value = true
    scoreError.value = ''
    try {
      const result = await bidMatchScoringApi.getLatestScore(tenderId)
      if (!result?.success) throw new Error(result?.message || '获取匹配评分失败')
      matchScore.value = result.data || null
    } catch (error) {
      scoreError.value = error?.response?.data?.message || error?.message || '获取匹配评分失败'
    } finally {
      scoreLoading.value = false
    }
  }

  const loadTenderDetail = async () => {
    const tenderId = route.params.id
    try {
      const result = await tendersApi.getDetail(tenderId)
      if (!result?.success) throw new Error(result?.message || '获取标讯详情失败')
      tender.value = result.data
      await loadMatchScore(tenderId)
    } catch (error) { ElMessage.error(error?.message || '网络请求失败，请稍后重试') }
  }

  const { handleParticipate, handleAbandon, handleViewOriginal } =
    useTenderActions(tender, loadTenderDetail)

  onMounted(async () => await loadTenderDetail())

  return {
    tender, matchScore,
    matchScoreState, scoreEmptyText, scoreEmptyDescription,
    regionMeta, industryMeta, deadlineParts,
    getScoreClass, getStatusType, getStatusText, getDeadlineClass,
    handleParticipate, handleViewOriginal, handleAbandon,
    loadMatchScore,
  }
}
