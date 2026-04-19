import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { useBarStore } from '@/stores/bar'
import { approvalApi, collaborationApi, knowledgeApi, projectsApi } from '@/api'
import { complianceApi, scoreAnalysisApi } from '@/api/modules/ai.js'
import { useProjectExpenseAggregation } from '@/components/project/composables/useProjectExpenseAggregation.js'
import { deliverableTypeMap } from './constants.js'
import { useProjectDetailAI } from './useProjectDetailAI.js'
import { useProjectDetailCore } from './useProjectDetailCore.js'
import { useProjectDetailDocuments } from './useProjectDetailDocuments.js'
import { useProjectDetailInit } from './useProjectDetailInit.js'
import { useProjectDetailProjectOps } from './useProjectDetailProjectOps.js'
import { useProjectDetailState } from './useProjectDetailState.js'
import { useProjectDetailTasks } from './useProjectDetailTasks.js'
import { useProjectDetailWorkflow } from './useProjectDetailWorkflow.js'

export function useProjectDetailPage() {
  const route = useRoute()
  const router = useRouter()
  const projectStore = useProjectStore()
  const userStore = useUserStore()
  const barStore = useBarStore()
  const isDemoMode = false
  const demoAutoTasks = ref([])
  const demoMobileCard = ref(null)
  const isApiProject = computed(() => !isDemoMode && /^\d+$/.test(String(route.params.id || '')))

  const context = {
    route, router, projectStore, userStore, barStore, projectsApi, collaborationApi, approvalApi, knowledgeApi, complianceApi, scoreAnalysisApi,
    isDemoMode, isApiProject, demoAutoTasks, demoMobileCard, deliverableTypeMap, message: ElMessage, confirm: ElMessageBox.confirm,
  }

  Object.assign(context, useProjectDetailState(context))
  Object.assign(context, useProjectExpenseAggregation({ projectStore, project: context.project, isApiProject }))
  Object.assign(context, useProjectDetailCore(context))
  Object.assign(context, useProjectDetailAI(context))
  Object.assign(context, useProjectDetailWorkflow(context))
  Object.assign(context, useProjectDetailProjectOps(context))
  Object.assign(context, useProjectDetailTasks(context))
  Object.assign(context, useProjectDetailDocuments(context))
  Object.assign(context, useProjectDetailInit(context))

  return context
}
