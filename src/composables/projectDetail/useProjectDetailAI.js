import { computed, ref } from 'vue'
import { complianceApi, projectQualityApi, scoreAnalysisApi } from '@/api/modules/ai.js'

function mapComplianceIssues(issues = []) {
  return issues.map((issue) => ({
    category: issue?.ruleType || issue?.severity || '合规',
    item: issue?.ruleName || issue?.description || '检查项',
    status: issue?.passed === false ? 'fail' : 'pass',
    suggestion: issue?.recommendation || issue?.description || '',
  }))
}

function buildScorePanel(analysis = {}) {
  const dimensions = Array.isArray(analysis?.dimensions) ? analysis.dimensions : []
  const findScore = (candidates) => {
    const matched = dimensions.find((dimension) =>
      candidates.some((candidate) => String(dimension?.dimensionName || dimension?.name || '').includes(candidate)),
    )
    return Number(matched?.score || 0)
  }

  return {
    total: Number(analysis?.overallScore || 0),
    tech: findScore(['技术能力', '技术方案', '技术']),
    business: findScore(['商务响应', '商务', '团队经验']),
    price: findScore(['价格竞争力', '价格', '报价']),
    qualification: findScore(['企业资质', '资质', '合规性']),
    comment: analysis?.summary || '',
    suggestions: dimensions.map((dimension) => dimension?.comments).filter(Boolean),
  }
}

export function useProjectDetailAI(context) {
  const { route, isDemoMode, isApiProject, project, message, state } = context
  const aiChecking = ref(false)
  const activeAITab = ref('compliance')
  const aiResult = ref({ compliance: null, quality: null, score: null })

  const hasAiCheckResult = computed(() => Boolean(
    aiResult.value.compliance || aiResult.value.quality || aiResult.value.score || project.value?.aiCheck?.compliance || project.value?.aiCheck?.quality,
  ))
  const canRunAICheck = computed(() => true)
  const showAICheckCard = computed(() => true)

  const toggleAssistantPanel = () => {
    state.assistantPanelVisible.value = !state.assistantPanelVisible.value
  }

  const openFlag = (target) => { target.value = true }
  const handleOpenScoreCoverage = () => message.info('评分点覆盖请查看项目创建页Step 4')

  const loadQualityResult = async () => {
    if (!isApiProject.value) {
      return null
    }
    const qualityResponse = await projectQualityApi.getProjectQualityResult(route.params.id)
    return qualityResponse?.data || null
  }

  const handleAdoptSuggestion = async (issue) => {
    const quality = aiResult.value.quality
    if (!quality?.id || !issue?.id) {
      return
    }
    const response = await projectQualityApi.adoptQualitySuggestion(route.params.id, quality.id, issue.id)
    aiResult.value = {
      ...aiResult.value,
      quality: response.data,
    }
    message.success('建议已采纳')
  }

  const handleIgnoreSuggestion = async (issueOrIndex) => {
    const quality = aiResult.value.quality
    if (!quality?.id) {
      return
    }
    const issue = typeof issueOrIndex === 'number'
      ? quality.errors?.[issueOrIndex]
      : issueOrIndex
    if (!issue?.id) {
      return
    }
    const response = await projectQualityApi.ignoreQualitySuggestion(route.params.id, quality.id, issue.id)
    aiResult.value = {
      ...aiResult.value,
      quality: response.data,
    }
    message.success('问题已忽略')
  }

  const runAICheck = async () => {
    aiChecking.value = true
    if (isDemoMode) {
      const issues = [
        { category: '资质', item: '营业执照年检', status: 'pass', suggestion: '资质有效' },
        { category: '资质', item: 'ISO认证有效期', status: 'pass', suggestion: '认证在有效期内' },
        { category: '响应', item: '技术参数偏离表', status: 'fail', suggestion: '第3项技术参数未响应，建议补充说明' },
        { category: '响应', item: '商务条款应答', status: 'pass', suggestion: '响应完整' },
      ]
      aiResult.value = {
        compliance: { score: Math.round((issues.filter((item) => item.status === 'pass').length / issues.length) * 100), issues },
        quality: {
          status: 'COMPLETED',
          empty: false,
          errors: [
            { id: 'demo-1', type: 'grammar', original: '投标文件已按要求提交。', suggestion: '投标文件已按要求提交并归档。', location: '摘要第1段' },
          ],
          suggestions: [{ id: 'demo-1', type: 'grammar', original: '投标文件已按要求提交。', suggestion: '投标文件已按要求提交并归档。', location: '摘要第1段' }],
        },
        score: { total: 87, tech: 90, business: 85, price: 82, qualification: 95, comment: '技术方案整体完整，商务应答较为充分。', suggestions: ['补充技术参数响应说明', '修正目录页码一致性'] },
      }
      aiChecking.value = false
      message.success('AI检查完成')
      return
    }

    if (!isApiProject.value) {
      aiResult.value = { compliance: null, quality: null, score: null }
      message.warning('当前项目ID不是后端真实ID，无法执行AI检查')
      aiChecking.value = false
      return
    }

    try {
      const [complianceResponse, scoreResponse, qualityRunResponse] = await Promise.all([
        complianceApi.getCheckResult(route.params.id),
        scoreAnalysisApi.getAnalysis(route.params.id),
        projectQualityApi.runProjectQualityCheck(route.params.id),
      ])
      const complianceRecord = Array.isArray(complianceResponse?.data) ? complianceResponse.data[0] : complianceResponse?.data
      aiResult.value = {
        compliance: complianceRecord ? { score: Number(complianceRecord.overallScore || complianceRecord.riskScore || 0), issues: mapComplianceIssues(complianceRecord.issues || []) } : null,
        quality: qualityRunResponse?.data || null,
        score: scoreResponse?.data ? buildScorePanel(scoreResponse.data) : null,
      }
      message.success(aiResult.value.quality?.empty ? '已完成检查，当前无可检查文档' : 'AI检查完成')
    } catch (error) {
      aiResult.value = { compliance: null, quality: null, score: null }
      message.error(error?.response?.data?.message || error?.message || 'AI检查失败')
    } finally {
      aiChecking.value = false
    }
  }

  loadQualityResult()
    .then((quality) => {
      if (quality) {
        aiResult.value = {
          ...aiResult.value,
          quality,
        }
      }
    })
    .catch(() => {})

  return {
    aiChecking,
    activeAITab,
    aiResult,
    hasAiCheckResult,
    canRunAICheck,
    showAICheckCard,
    toggleAssistantPanel,
    handleOpenCompetitionIntel: () => openFlag(state.showCompetitionIntel),
    handleOpenRoiAnalysis: () => openFlag(state.showROIAnalysis),
    handleOpenScoreCoverage,
    handleOpenComplianceCheck: () => openFlag(state.showComplianceCheck),
    handleOpenVersionControl: () => openFlag(state.showVersionControl),
    handleOpenCollaboration: () => openFlag(state.showCollaboration),
    handleOpenAutoTasks: () => openFlag(state.showAutoTasks),
    handleOpenMobileCard: () => openFlag(state.showMobileCard),
    handleAdoptSuggestion,
    handleIgnoreSuggestion,
    runAICheck,
  }
}
