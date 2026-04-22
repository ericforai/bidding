import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { tendersApi } from '@/api'
import { safeTenderUrl } from '../bidding-utils.js'
import { getTenderStatusTagType, getTenderStatusText } from '../bidding-utils-status.js'

export function useBiddingDetailPage() {
  const router = useRouter()
  const route = useRoute()
  const showTenderAiSection = true

  const tender = ref(null)
  const isFollowed = ref(false)

  const probabilityRate = computed(() => {
    if (!tender.value) return 0
    const score = tender.value.aiScore
    if (score >= 90) return 5
    if (score >= 80) return 4
    if (score >= 70) return 3
    if (score >= 60) return 2
    return 1
  })

  const advantages = computed(() => {
    if (!tender.value) return []
    const advantageList = []
    if (tender.value.aiScore >= 90) {
      advantageList.push('该客户历史合作记录良好，累计中标3次')
      advantageList.push('我司在信创领域具有较强技术优势')
      advantageList.push('拥有相关行业成功案例')
    } else if (tender.value.aiScore >= 80) {
      advantageList.push('传统优势领域，有行业经验')
      advantageList.push('前期已建立良好客户关系')
    } else {
      advantageList.push('预算充足，项目规模适中')
      advantageList.push('技术要求在现有能力范围内')
    }
    return advantageList
  })

  const suggestions = computed(() => {
    if (!tender.value) return []
    return [
      {
        title: '投标策略建议',
        type: 'success',
        content: tender.value.aiReason || '建议优先跟进，预计中标概率较高',
      },
      {
        title: '注意事项',
        type: 'warning',
        content: '需提前准备相关资质文件，确保符合招标要求',
      },
    ]
  })

  const relatedCases = computed(() => {
    if (!tender.value) return []
    const mockCases = {
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
    return mockCases[tender.value.industry] || mockCases.政府
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

  onMounted(async () => {
    const tenderId = route.params.id
    try {
      const result = await tendersApi.getDetail(tenderId)
      if (result?.success) {
        tender.value = result.data
      } else {
        ElMessage.error(result?.message || '获取标讯详情失败')
      }
    } catch (error) {
      console.error('Failed to fetch tender detail:', error)
      ElMessage.error('网络请求失败，请稍后重试')
    }
  })

  return {
    showTenderAiSection,
    tender,
    isFollowed,
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
  }
}
