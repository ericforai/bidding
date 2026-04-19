import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api'
import { useUserStore } from '@/stores/user'
import {
  buildKnowledgeQuery,
  buildReferencePayload,
  mergeSectionSourceMetadata,
  normalizeKnowledgeMatch
} from './documentEditorHelpers.js'

function buildKnowledgeRelevance(item = {}, keyword = '') {
  const text = `${item.title || ''} ${item.summary || ''} ${item.content || ''}`.toLowerCase()
  const tokens = String(keyword || '')
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)

  if (tokens.length === 0) {
    return Number(item.useCount || item.viewCount || item.relevance || 0)
  }

  return tokens.reduce((score, token) => {
    if (text.includes(token)) return score + 30
    return score
  }, Number(item.useCount || item.viewCount || item.relevance || 0))
}

function normalizeCaseMatch(item = {}, keyword = '') {
  return normalizeKnowledgeMatch({
    id: item.id,
    type: 'case',
    title: item.title || '未命名案例',
    summary: item.summary || item.description || '',
    content: item.summary || item.description || '',
    relevance: Math.min(100, buildKnowledgeRelevance(item, keyword)),
    sourceLabel: '案例库',
    sourceDetail: [item.customer || item.customerName, item.industry, item.year].filter(Boolean).join(' · ')
  })
}

function normalizeTemplateMatch(item = {}, keyword = '') {
  return normalizeKnowledgeMatch({
    id: item.id,
    type: 'template',
    title: item.name || item.title || '未命名模板',
    summary: item.description || item.summary || '',
    content: item.content || item.templateContent || '',
    relevance: Math.min(100, buildKnowledgeRelevance(item, keyword)),
    sourceLabel: '模板库',
    sourceDetail: [item.category, item.version].filter(Boolean).join(' · ')
  })
}

export function useDocumentKnowledge({
  currentSection,
  projectInfo,
  documentInfo,
  isRemoteProjectId
}) {
  const userStore = useUserStore()
  const knowledgeMatches = ref([])

  async function loadKnowledgeMatches(section) {
    const targetSection = section || currentSection.value

    if (!targetSection) {
      knowledgeMatches.value = []
      return []
    }

    if (!isRemoteProjectId.value) {
      knowledgeMatches.value = []
      return []
    }

    const query = buildKnowledgeQuery(targetSection, documentInfo.value)

    try {
      const [caseResult, templateResult] = await Promise.all([
        knowledgeApi.cases.getList({ keyword: query.keyword }),
        knowledgeApi.templates.getList({ name: query.keyword })
      ])

      const caseMatches = Array.isArray(caseResult?.data)
        ? caseResult.data.slice(0, 4).map((item) => normalizeCaseMatch(item, query.keyword))
        : []
      const templateMatches = Array.isArray(templateResult?.data)
        ? templateResult.data.slice(0, 4).map((item) => normalizeTemplateMatch(item, query.keyword))
        : []

      knowledgeMatches.value = [...caseMatches, ...templateMatches]
        .sort((a, b) => b.relevance - a.relevance)
        .slice(0, 5)

      return knowledgeMatches.value
    } catch (error) {
      knowledgeMatches.value = []
      return []
    }
  }

  async function handleInsertKnowledge(match) {
    const section = currentSection.value
    if (!section || !match) return

    const insertedAt = new Date().toISOString()
    const sourceLabel = match.type === 'case' ? '案例库' : '模板库'

    if (match.type === 'case' && /^\d+$/.test(String(match.id))) {
      try {
        const payload = buildReferencePayload(match, userStore.currentUser || {}, section, projectInfo.value || {})
        const response = await knowledgeApi.cases.createReferenceRecord(match.id, payload)
        const referenceData = response?.data || {}
        const citation = {
          kind: 'case',
          title: match.title,
          sourceLabel,
          sourceDetail: match.sourceDetail || '',
          referenceId: referenceData.id ?? null,
          referenceTarget: payload.referenceTarget,
          referenceContext: payload.referenceContext,
          referencedAt: insertedAt
        }

        mergeSectionSourceMetadata(section, citation)
        section.content = `${section.content || ''}\n\n> 来源：${sourceLabel} · ${match.title}\n> 引用记录：${citation.referenceId || '创建成功'}\n\n${match.content || ''}\n`
        ElMessage.success('案例已插入并记录引用')
      } catch (error) {
        const citation = {
          kind: 'case',
          title: match.title,
          sourceLabel,
          sourceDetail: match.sourceDetail || '',
          referenceId: null,
          referenceTarget: section.name || match.title,
          referenceContext: `文档编辑器插入案例：${section.name || '未命名章节'}`,
          referencedAt: insertedAt
        }

        mergeSectionSourceMetadata(section, citation)
        section.content = `${section.content || ''}\n\n> 来源：${sourceLabel} · ${match.title}\n\n${match.content || ''}\n`
        ElMessage.warning('案例已插入，但引用记录创建失败')
      }

      return
    }

    const citation = {
      kind: match.type || 'template',
      title: match.title,
      sourceLabel,
      sourceDetail: match.sourceDetail || '',
      referencedAt: insertedAt
    }

    mergeSectionSourceMetadata(section, citation)
    section.content = `${section.content || ''}\n\n> 来源：${sourceLabel} · ${match.title}\n\n${match.content || ''}\n`
    ElMessage.success('知识内容已插入')
  }

  return {
    knowledgeMatches,
    loadKnowledgeMatches,
    handleInsertKnowledge
  }
}
