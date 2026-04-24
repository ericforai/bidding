import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { bidMatchScoringApi, tendersApi } from '@/api'
import {
  buildWinProbabilityView,
  formatTenderDisplayField,
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
  const industryMeta = computed(() => formatTenderDisplayField(tender.value?.industry))
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

  const relatedCases = computed(() => {
    if (!tender.value) return []
    const caseCatalog = {
      政府: [
        {
          id: 'C001',
          title: '某省政府OA办公系统',
          customer: '某省政府',
          amount: 300,
          year: 2024,
          summary: '为省政府打造一体化办公平台，包括公文管理、会议管理、日程管理等核心功能',
          highlights: ['信创适配', '高并发处理', '移动端支持'],
        },
      ],
      能源: [
        {
          id: 'C002',
          title: '华东电网信息化项目',
          customer: '华东电网',
          amount: 800,
          year: 2024,
          summary: '电网企业ERP系统升级及数据中台建设',
          highlights: ['微服务架构', '数据治理', '智能报表'],
        },
      ],
      交通: [
        {
          id: 'C003',
          title: '西部智慧园区项目',
          customer: '西部某园区',
          amount: 500,
          year: 2023,
          summary: '智慧园区综合管理平台',
          highlights: ['IoT集成', '3D可视化', '能耗分析'],
        },
      ],
      数据中心: [
        {
          id: 'C004',
          title: '某银行数据中心建设',
          customer: '某商业银行',
          amount: 1500,
          year: 2024,
          summary: '银行级数据中心基础设施建设',
          highlights: ['高可用架构', '安全合规', '绿色节能'],
        },
      ],
    }
    return caseCatalog[tender.value.industry] || caseCatalog.政府
  })

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
      await loadMatchScore(tenderId)
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
