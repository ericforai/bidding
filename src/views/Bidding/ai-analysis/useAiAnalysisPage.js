import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiApi, tendersApi } from '@/api'

const dimensionDetailsMap = {
  客户关系: {
    score: 80,
    description: '与客户有历史合作记录，关系维护良好，但近期高层互动较少',
    suggestion: '建议安排分管领导或高层进行技术交流，加深客户印象',
  },
  需求匹配: {
    score: 70,
    description: '核心业务领域匹配，但部分新技术要求需要补充技术储备',
    suggestion: '梳理现有技术方案，结合新要求进行针对性优化',
  },
  资质满足: {
    score: 60,
    description: '基础资质齐全，但部分高等级资质缺失或即将过期',
    suggestion: '及时更新即将过期的资质，考虑与合作方联合投标',
  },
  交付能力: {
    score: 85,
    description: '具备完善的交付团队和成功案例，交付风险较低',
    suggestion: '保持优势，提前组建项目团队进行资源准备',
  },
  竞争态势: {
    score: 50,
    description: '竞争对手实力较强，预计至少3-5家优质竞争对手',
    suggestion: '分析竞争对手特点，制定差异化竞争策略',
  },
}

export function useAiAnalysisPage() {
  const router = useRouter()
  const route = useRoute()

  const tenderId = ref(route.params.id || 'T001')
  const analysisData = ref(null)
  const tenderInfo = ref(null)
  const expandAll = ref(false)
  const activeDimensions = ref([])
  const showParsingDialog = ref(false)
  const parseProgress = ref(0)
  const parseTimer = ref(null)
  const loadError = ref('')

  const progressColors = [
    { color: '#f56c6c', percentage: 30 },
    { color: '#e6a23c', percentage: 60 },
    { color: '#409eff', percentage: 90 },
    { color: '#67c23a', percentage: 100 },
  ]

  const dimensionDetails = computed(() => {
    if (!analysisData.value) return []
    return analysisData.value.dimensionScores.map((dim) => ({
      name: dim.name,
      score: dim.score,
      ...(dimensionDetailsMap[dim.name] || {
        description: '暂无维度说明',
        suggestion: '暂无改进建议',
      }),
    }))
  })

  const getScoreColor = (score) => {
    if (score >= 71) return '#67c23a'
    if (score >= 41) return '#e6a23c'
    return '#f56c6c'
  }

  const getScoreLevelClass = (score) => {
    if (score >= 80) return 'score-excellent'
    if (score >= 60) return 'score-good'
    return 'score-normal'
  }

  const getDimensionTagType = (score) => {
    if (score >= 71) return 'success'
    if (score >= 41) return 'warning'
    return 'danger'
  }

  const getPriorityTagType = (priority) => (priority === 'high' ? 'danger' : 'warning')

  const handleGoBack = () => {
    router.back()
  }

  const handleExport = () => {
    ElMessage.info('报告导出能力将在后续版本开放，本轮先提供在线查看结果。')
  }

  const handleSyncTasks = () => {
    ElMessage.info('任务同步能力将在后续版本开放，本轮先提供任务建议查看。')
  }

  const handleTaskCheck = (task) => {
    if (task.completed) {
      ElMessage.success(`任务"${task.title}"已完成`)
    }
  }

  const handleAddToPool = () => {
    ElMessageBox.confirm('确定要加入意向池吗？加入后可以在投标项目列表中查看。', '加入意向池', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
      .then(() => {
        ElMessage.success('已加入意向池')
        router.push('/bidding')
      })
      .catch(() => {})
  }

  const handleCreateProject = () => {
    ElMessageBox.confirm('确定要创建投标项目吗？创建后将进入项目立项流程。', '创建投标项目', {
      confirmButtonText: '确定创建',
      cancelButtonText: '取消',
      type: 'success',
    })
      .then(() => {
        ElMessage.success('正在跳转到项目创建页...')
        router.push({
          path: '/project/create',
          query: { tenderId: tenderId.value },
        })
      })
      .catch(() => {})
  }

  const stopParsingAnimation = () => {
    if (parseTimer.value) {
      clearInterval(parseTimer.value)
      parseTimer.value = null
    }
  }

  const startParsingAnimation = () => {
    stopParsingAnimation()
    showParsingDialog.value = true
    parseProgress.value = 0

    parseTimer.value = setInterval(() => {
      if (parseProgress.value < 100) {
        parseProgress.value += Math.random() * 15 + 5
        if (parseProgress.value > 100) {
          parseProgress.value = 100
        }
      } else {
        stopParsingAnimation()
        setTimeout(() => {
          showParsingDialog.value = false
        }, 500)
      }
    }, 800)
  }

  const showLoadError = (message) => {
    loadError.value = `加载失败：${message}`
    ElMessage.error({
      message: loadError.value,
      duration: 10000,
      showClose: true,
    })
  }

  const loadTenderInfo = async () => {
    try {
      const response = await tendersApi.getDetail(tenderId.value)
      if (response?.success && response.data) {
        tenderInfo.value = response.data
        return
      }
      tenderInfo.value = null
      showLoadError(response?.message || '标讯信息加载失败')
    } catch (error) {
      tenderInfo.value = null
      showLoadError(error?.response?.data?.message || error?.message || '标讯信息加载失败')
    }
  }

  const loadAnalysis = async () => {
    try {
      const response = await aiApi.bid.getAnalysis(tenderId.value)
      if (response?.success && response.data) {
        analysisData.value = response.data
        return
      }
      analysisData.value = null
      showLoadError(response?.message || 'AI 分析数据加载失败')
    } catch (error) {
      analysisData.value = null
      showLoadError(error?.response?.data?.message || error?.message || 'AI 分析数据加载失败')
    }
  }

  const initializePage = async () => {
    loadError.value = ''
    const showParsing = !route.params.fromList
    if (showParsing) {
      startParsingAnimation()
    }

    await loadTenderInfo()

    if (showParsing) {
      await new Promise((resolve) => setTimeout(resolve, 1000))
    }

    await loadAnalysis()
  }

  onMounted(() => {
    initializePage()
  })

  onBeforeUnmount(() => {
    stopParsingAnimation()
  })

  return {
    tenderInfo,
    analysisData,
    expandAll,
    activeDimensions,
    showParsingDialog,
    parseProgress,
    loadError,
    progressColors,
    dimensionDetails,
    getScoreColor,
    getScoreLevelClass,
    getDimensionTagType,
    getPriorityTagType,
    handleGoBack,
    handleExport,
    handleSyncTasks,
    handleTaskCheck,
    handleAddToPool,
    handleCreateProject,
  }
}
