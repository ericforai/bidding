import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { bidMatchScoringApi, knowledgeApi, tendersApi } from '@/api'
import {
  buildWinProbabilityView,
  formatTenderDisplayField,
  formatTenderIndustry,
  getTenderDateTimeParts,
  safeTenderUrl,
} from '../bidding-utils.js'
import { getTenderStatusTagType, getTenderStatusText } from '../bidding-utils-status.js'
import { normalizeMatchScoreForView, summarizeScoreState } from '../match-scoring/normalizers.js'

export function useBiddingDetailPage() {
  const router = useRouter()
  const route = useRoute()
  const showTenderAiSection = true

  const tender = ref(null)
  const isFollowed = ref(false)
  const matchScore = ref(null)
  const relatedCases = ref([])
  const relatedCasesLoading = ref(false)
  const scoreLoading = ref(false)
  const scoreGenerating = ref(false)
  const scoreError = ref('')

  const scoreForView = computed(() => normalizeMatchScoreForView(matchScore.value))
  const scoreSummary = computed(() => summarizeScoreState({
    loading: scoreLoading.value,
    generating: scoreGenerating.value,
    error: scoreError.value,
    score: scoreForView.value,
  }))
  const matchScoreState = computed(() => scoreSummary.value.state)
  const scoreEmptyText = computed(() => scoreSummary.value.actionText || scoreSummary.value.text)
  const scoreEmptyDescription = computed(() => scoreSummary.value.description)

  const regionMeta = computed(() => formatTenderDisplayField(tender.value?.region))
  const industryMeta = computed(() => formatTenderIndustry(tender.value?.industry))
  const deadlineParts = computed(() => getTenderDateTimeParts(tender.value?.deadline))
  const winProbabilityView = computed(() => {
    const score = scoreForView.value?.totalScore ?? tender.value?.aiScore ?? 0
    return buildWinProbabilityView(score)
  })
  const probabilityRate = computed(() => winProbabilityView.value.rate)

  const advantages = computed(() => {
    const dimensions = scoreForView.value?.dimensionSummaries || []
    return dimensions
      .filter((dimension) => dimension.score >= 70)
      .slice(0, 4)
      .map((dimension) => `${dimension.name}当前得分 ${dimension.score} 分，可作为投标策略重点。`)
  })

  const suggestions = computed(() => {
    const summary = scoreForView.value?.summary || tender.value?.aiReason
    if (!summary && advantages.value.length === 0) return []
    return [
      {
        title: '评分建议',
        type: 'success',
        content: summary || '请结合高分维度组织投标响应材料。',
      },
      {
        title: '跟进提醒',
        type: 'warning',
        content: '请核对评分证据与招标文件要求，缺口项应在立项前闭环。',
      },
    ]
  })

  const normalizeCaseIndustryQuery = (industry) => ({
    政府: 'government',
    能源: 'energy',
    交通: 'transport',
    制造业: 'manufacturing',
    教育: 'education',
    医疗: 'healthcare',
    互联网: 'internet',
    GOVERNMENT: 'government',
    ENERGY: 'energy',
    TRANSPORTATION: 'transport',
    MANUFACTURING: 'manufacturing',
  }[String(industry || '').trim()] || undefined)

  const getScoreClass = (score) => {
    if (score >= 90) return 'score-excellent'
    if (score >= 80) return 'score-good'
    return 'score-normal'
  }

  const getStatusType = (status) => getTenderStatusTagType(status)
  const getStatusText = (status) => getTenderStatusText(status)

  const getDeadlineClass = (deadline) => {
    const today = new Date()
    const deadlineDate = new Date(deadline)
    const diffDays = Math.ceil((deadlineDate - today) / (1000 * 60 * 60 * 24))
    if (diffDays <= 3) return 'deadline-urgent'
    if (diffDays <= 7) return 'deadline-warning'
    return ''
  }

  const handleParticipate = () => {
    ElMessage.success('正在跳转到项目创建页...')
    router.push({
      path: '/project/create',
      query: { tenderId: tender.value.id },
    })
  }

  const handleFollow = () => {
    isFollowed.value = !isFollowed.value
    ElMessage.success(isFollowed.value ? '已加入关注' : '已取消关注')
  }

  const handleShare = () => {
    ElMessage.success('分享链接已复制到剪贴板')
  }

  const handleViewOriginal = () => {
    const url = safeTenderUrl(tender.value?.originalUrl)
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer')
    } else {
      ElMessage.warning('该标讯暂无官网公告链接')
    }
  }

  const handleViewCase = (caseId) => {
    router.push({
      path: '/knowledge/case/detail',
      query: { id: caseId },
    })
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

  const loadRelatedCases = async () => {
    if (!tender.value) return
    relatedCasesLoading.value = true
    try {
      const keyword = tender.value.industry || tender.value.purchaserName || tender.value.title || ''
      const result = await knowledgeApi.cases.getList({
        keyword,
        industry: normalizeCaseIndustryQuery(tender.value.industry),
        page: 1,
        pageSize: 4,
        sort: 'recent',
      })
      relatedCases.value = result?.success && Array.isArray(result.data) ? result.data : []
    } catch {
      relatedCases.value = []
    } finally {
      relatedCasesLoading.value = false
    }
  }

  const handleGenerateMatchScore = async () => {
    const tenderId = tender.value?.id || route.params.id
    if (!tenderId) return

    scoreGenerating.value = true
    scoreError.value = ''
    try {
      const result = await bidMatchScoringApi.generateScore(tenderId)
      if (!result?.success) throw new Error(result?.message || '生成匹配评分失败')
      matchScore.value = result.data || null
      await loadMatchScore(tenderId)
      ElMessage.success('匹配评分已生成')
    } catch (error) {
      scoreError.value = error?.response?.data?.message || error?.message || '生成匹配评分失败'
      ElMessage.error(scoreError.value)
    } finally {
      scoreGenerating.value = false
    }
  }

  const handleConfigureMatchScore = () => {
    router.push({
      path: '/settings',
      query: { tab: 'bid-match-scoring' },
    })
  }

  const loadTenderDetail = async () => {
    const tenderId = route.params.id
    try {
      const result = await tendersApi.getDetail(tenderId)
      if (!result?.success) throw new Error(result?.message || '获取标讯详情失败')
      tender.value = result.data
      await Promise.all([loadMatchScore(tenderId), loadRelatedCases()])
    } catch (error) {
      ElMessage.error(error?.message || '网络请求失败，请稍后重试')
    }
  }

  onMounted(async () => {
    await loadTenderDetail()
  })

  return {
    showTenderAiSection,
    tender,
    isFollowed,
    matchScore,
    scoreLoading,
    scoreGenerating,
    scoreError,
    matchScoreState,
    scoreEmptyText,
    scoreEmptyDescription,
    regionMeta,
    industryMeta,
    deadlineParts,
    winProbabilityView,
    probabilityRate,
    advantages,
    suggestions,
    relatedCases,
    relatedCasesLoading,
    getScoreClass,
    getStatusType,
    getStatusText,
    getDeadlineClass,
    handleParticipate,
    handleFollow,
    handleShare,
    handleViewOriginal,
    handleViewCase,
    loadMatchScore,
    handleGenerateMatchScore,
    handleConfigureMatchScore,
  }
}
