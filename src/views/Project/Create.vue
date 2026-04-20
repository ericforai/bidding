<template>
  <div class="project-create-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">{{ pageTitle }}</span>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>

      <el-steps :active="currentStep" finish-status="success" align-center class="steps">
        <el-step title="基本信息" description="从CRM同步或手动输入" />
        <el-step title="项目详情" description="完善项目信息" />
        <el-step title="任务分解" description="添加项目任务" />
        <el-step v-if="hasAiStep" title="智能辅助" description="AI分析与建议" />
      </el-steps>

      <div class="step-content">
        <!-- 步骤1: 基本信息 -->
        <div v-show="currentStep === 0" class="step-panel">
          <el-form ref="basicFormRef" :model="basicForm" :rules="basicRules" label-width="120px">
            <el-alert
              title="提示：可以从CRM系统同步客户信息，或手动填写以下信息"
              type="info"
              :closable="false"
              show-icon
              class="mb-16"
            />

            <el-form-item label="CRM同步" prop="syncFromCRM">
              <el-button type="primary" :icon="Refresh" @click="syncFromCRM" :loading="syncing">
                从CRM同步客户信息
              </el-button>
              <span v-if="syncedFromCRM" class="sync-tip">已从CRM同步</span>
            </el-form-item>

            <el-divider content-position="left">基本信息</el-divider>

            <el-form-item label="项目名称" prop="name">
              <el-input v-model="basicForm.name" placeholder="请输入项目名称" clearable />
            </el-form-item>

            <el-form-item label="客户名称" prop="customer">
              <el-input v-model="basicForm.customer" placeholder="请输入客户名称" clearable />
            </el-form-item>

            <el-form-item label="预算(万元)" prop="budget">
              <el-input-number
                v-model="basicForm.budget"
                :min="0"
                :precision="2"
                :step="10"
                controls-position="right"
                style="width: 200px"
              />
            </el-form-item>

            <el-form-item label="行业" prop="industry">
              <el-select v-model="basicForm.industry" placeholder="请选择行业" clearable>
                <el-option label="政府" value="政府" />
                <el-option label="能源" value="能源" />
                <el-option label="交通" value="交通" />
                <el-option label="金融" value="金融" />
                <el-option label="教育" value="教育" />
                <el-option label="医疗" value="医疗" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>

            <el-form-item label="地区" prop="region">
              <el-input v-model="basicForm.region" placeholder="请输入地区" clearable />
            </el-form-item>

            <el-form-item label="招标平台" prop="platform">
              <el-select
                v-model="basicForm.platform"
                placeholder="请选择招标平台"
                filterable
                allow-create
                clearable
                @change="handlePlatformChange"
                style="width: 100%"
              >
                <el-option
                  v-for="site in platformOptions"
                  :key="site.id"
                  :label="site.name"
                  :value="site.name"
                >
                  <span>{{ site.name }}</span>
                  <span style="float: right; color: #8492a6; font-size: 12px">{{ site.region }}</span>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="投标截止日期" prop="deadline">
              <el-date-picker
                v-model="basicForm.deadline"
                type="date"
                placeholder="请选择日期"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledDate"
              />
            </el-form-item>

            <el-form-item label="项目负责人" prop="manager">
              <el-select v-model="basicForm.manager" placeholder="请选择负责人">
                <el-option
                  v-for="user in userList"
                  :key="user.id"
                  :label="user.name"
                  :value="user.name"
                />
              </el-select>
            </el-form-item>

            <el-divider content-position="left">竞争对手信息</el-divider>

            <el-form-item label="竞争对手">
              <el-select
                v-model="basicForm.competitors"
                multiple
                filterable
                allow-create
                placeholder="选择或输入竞争对手名称"
                style="width: 100%"
                @change="handleCompetitorsChange"
              >
                <el-option
                  v-for="item in competitorOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>

            <el-form-item
              label="竞争分析"
              v-if="competitorAnalysis.length > 0"
              class="competitor-analysis-item"
            >
              <el-table :data="competitorAnalysis" size="small" border>
                <el-table-column prop="name" label="竞争对手" width="150" />
                <el-table-column prop="strength" label="优势分析" width="200">
                  <template #default="{ row }">
                    <el-input v-model="row.strength" placeholder="优势分析" size="small" />
                  </template>
                </el-table-column>
                <el-table-column prop="weakness" label="劣势分析" width="200">
                  <template #default="{ row }">
                    <el-input v-model="row.weakness" placeholder="劣势分析" size="small" />
                  </template>
                </el-table-column>
                <el-table-column prop="winRate" label="历史中标率" width="150">
                  <template #default="{ row }">
                    <div class="win-rate-input">
                      <el-input-number
                        v-model="row.winRate"
                        :min="0"
                        :max="100"
                        :precision="0"
                        size="small"
                        controls-position="right"
                      />
                      <span>%</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="history" label="历史中标项目" min-width="200">
                  <template #default="{ row }">
                    <el-input
                      v-model="row.history"
                      placeholder="历史中标项目，用逗号分隔"
                      size="small"
                      type="textarea"
                      :rows="2"
                    />
                  </template>
                </el-table-column>
              </el-table>
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤2: 项目详情 -->
        <div v-show="currentStep === 1" class="step-panel">
          <el-form ref="detailFormRef" :model="detailForm" :rules="detailRules" label-width="120px">
            <el-divider content-position="left">项目详情</el-divider>

            <el-form-item label="项目描述" prop="description">
              <el-input
                v-model="detailForm.description"
                type="textarea"
                :rows="4"
                placeholder="请输入项目描述、需求概述等"
              />
            </el-form-item>

            <el-form-item label="项目标签" prop="tags">
              <el-select
                v-model="detailForm.tags"
                multiple
                filterable
                allow-create
                placeholder="请选择或输入标签"
              >
                <el-option label="智慧办公" value="智慧办公" />
                <el-option label="信创" value="信创" />
                <el-option label="大数据" value="大数据" />
                <el-option label="云计算" value="云计算" />
                <el-option label="物联网" value="物联网" />
                <el-option label="AI" value="AI" />
                <el-option label="高优先级" value="高优先级" />
              </el-select>
            </el-form-item>

            <el-form-item label="预计开工日期">
              <el-date-picker
                v-model="detailForm.startDate"
                type="date"
                placeholder="请选择预计开工日期"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>

            <el-form-item label="预计完工日期">
              <el-date-picker
                v-model="detailForm.endDate"
                type="date"
                placeholder="请选择预计完工日期"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>

            <el-form-item label="备注">
              <el-input
                v-model="detailForm.remark"
                type="textarea"
                :rows="3"
                placeholder="其他备注信息"
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤3: 任务分解 -->
        <div v-show="currentStep === 2" class="step-panel">
          <el-form ref="taskFormRef" :model="taskForm" label-width="120px">
            <el-divider content-position="left">任务分解</el-divider>

            <div class="task-list">
              <div
                v-for="(task, index) in taskForm.tasks"
                :key="index"
                class="task-item"
              >
                <el-card>
                  <template #header>
                    <div class="task-header">
                      <span>任务 {{ index + 1 }}</span>
                      <el-button
                        link
                        type="danger"
                        :icon="Delete"
                        @click="removeTask(index)"
                        v-if="taskForm.tasks.length > 1"
                      >
                        删除
                      </el-button>
                    </div>
                  </template>
                  <el-form :model="task" label-width="100px">
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="任务名称">
                          <el-input v-model="task.name" placeholder="请输入任务名称" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="负责人">
                          <el-select v-model="task.owner" placeholder="请选择负责人">
                            <el-option
                              v-for="user in userList"
                              :key="user.id"
                              :label="user.name"
                              :value="user.name"
                            />
                          </el-select>
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="截止日期">
                          <el-date-picker
                            v-model="task.deadline"
                            type="date"
                            placeholder="请选择日期"
                            value-format="YYYY-MM-DD"
                            style="width: 100%"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="优先级">
                          <el-select v-model="task.priority" placeholder="请选择优先级">
                            <el-option label="高" value="high" />
                            <el-option label="中" value="medium" />
                            <el-option label="低" value="low" />
                          </el-select>
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-form>
                </el-card>
              </div>
            </div>

            <el-button :icon="Plus" @click="addTask" class="add-task-btn">
              添加任务
            </el-button>
          </el-form>
        </div>

        <!-- 步骤4: 智能辅助 -->
        <div v-if="hasAiStep" v-show="currentStep === 3" class="step-panel">
          <el-alert
            title="AI智能分析"
            type="success"
            :closable="false"
            show-icon
            class="mb-16"
          >
            <template #default>
              <div class="ai-loading" v-if="analyzing">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>AI正在分析项目数据...</span>
              </div>
              <div v-else class="ai-summary">
                <div class="summary-header">
                  <div class="win-score">
                    <span class="score-label">赢面评分</span>
                    <span class="score-value" :class="getWinScoreClass(aiSummary.winScore)">
                      {{ aiSummary.winScore }}
                    </span>
                    <span class="score-max">/100</span>
                  </div>
                  <div class="win-level">
                    <el-tag :type="getWinLevelType(aiSummary.winLevel)" size="large">
                      {{ getWinLevelText(aiSummary.winLevel) }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </template>
          </el-alert>

          <!-- 关键风险 -->
          <el-divider content-position="left">
            <el-icon><Warning /></el-icon>
            关键风险
          </el-divider>
          <div class="risk-list">
            <div
              v-for="(risk, index) in aiSummary.risks"
              :key="index"
              class="risk-item"
              :class="'risk-' + risk.level"
            >
              <el-icon class="risk-icon">
                <WarningFilled v-if="risk.level === 'high'" />
                <Warning v-else />
              </el-icon>
              <span class="risk-content">{{ risk.content }}</span>
              <el-tag :type="risk.level === 'high' ? 'danger' : 'warning'" size="small">
                {{ risk.level === 'high' ? '高风险' : '中风险' }}
              </el-tag>
            </div>
          </div>

          <!-- AI建议 -->
          <el-divider content-position="left">
            <el-icon><MagicStick /></el-icon>
            AI建议
          </el-divider>
          <FeaturePlaceholder
            v-if="scorePreviewPlaceholder"
            compact
            :title="scorePreviewPlaceholder.title"
            :message="scorePreviewPlaceholder.message"
            :hint="scorePreviewPlaceholder.hint"
          />
          <ul class="suggestion-list">
            <li v-for="(suggestion, index) in aiSummary.suggestions" :key="index">
              {{ suggestion }}
            </li>
          </ul>

          <!-- 评分点覆盖率组件 -->
          <el-divider content-position="left">
            <el-icon><DataAnalysis /></el-icon>
            评分点覆盖率
          </el-divider>
          <ScoreCoverage
            :score-categories="scoreAnalysis.scoreCategories"
            :gap-items="scoreAnalysis.gapItems"
          />

          <!-- 自动生成任务清单 -->
          <el-divider content-position="left">
            <el-icon><List /></el-icon>
            AI生成任务清单
          </el-divider>
          <div class="ai-tasks">
            <el-alert
              title="以下任务由AI根据评分缺口自动生成，您可以编辑调整"
              type="info"
              :closable="false"
              show-icon
              class="mb-12"
            />
            <div
              v-for="(aiTask, index) in aiGeneratedTasks"
              :key="index"
              class="ai-task-item"
            >
              <el-checkbox v-model="aiTask.selected" :label="aiTask.name" />
              <div class="ai-task-meta">
                <el-tag size="small" :type="getPriorityType(aiTask.priority)">
                  {{ getPriorityText(aiTask.priority) }}
                </el-tag>
                <span class="ai-task-suggest">{{ aiTask.suggestion }}</span>
              </div>
            </div>
          </div>

          <!-- 确认提示 -->
          <el-alert
            title="确认以上信息无误后，点击下方按钮完成项目创建"
            type="success"
            :closable="false"
            show-icon
            class="mt-16"
          />
        </div>
      </div>

      <div class="step-actions">
        <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
        <el-button v-if="currentStep < lastStepIndex" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="currentStep === lastStepIndex" type="primary" :loading="submitting" @click="handleSubmit">
          确认并创建项目
        </el-button>
      </div>
    </el-card>

    <!-- 资产检查弹窗 -->
    <el-dialog
      v-model="showAssetCheckDialog"
      title="投标资产检查"
      width="500px"
      :close-on-click-modal="false"
    >
      <div v-if="assetCheckResult" class="asset-check-result">
        <div class="check-header">
          <span class="site-name">{{ assetCheckResult.site?.name }}</span>
          <el-tag
            v-if="assetCheckResult.capability?.status === 'available'"
            type="success"
            size="large"
          >
            可投标
          </el-tag>
          <el-tag
            v-else-if="assetCheckResult.capability?.status === 'risk'"
            type="warning"
            size="large"
          >
            有风险
          </el-tag>
          <el-tag v-else type="danger" size="large">
            不可投标
          </el-tag>
        </div>

        <div class="check-items">
          <div class="check-item">
            <el-icon
              :class="assetCheckResult.capability?.hasAccount ? 'icon-success' : 'icon-error'"
            >
              <component :is="assetCheckResult.capability?.hasAccount ? 'CircleCheck' : 'CircleClose'" />
            </el-icon>
            <span>账号：{{ assetCheckResult.capability?.hasAccount ? '已注册' : '未注册' }}</span>
          </div>
          <div class="check-item">
            <el-icon
              :class="assetCheckResult.capability?.hasAvailableUK ? 'icon-success' : 'icon-error'"
            >
              <component :is="assetCheckResult.capability?.hasAvailableUK ? 'CircleCheck' : 'CircleClose'" />
            </el-icon>
            <span>UK：{{ assetCheckResult.capability?.ukCount > 0 ? (assetCheckResult.capability?.hasAvailableUK ? '在库' : '已借出') : '不需要' }}</span>
          </div>
          <div v-if="assetCheckResult.capability?.primaryOwner" class="check-item">
            <el-icon class="icon-user"><User /></el-icon>
            <span>责任人：{{ assetCheckResult.capability?.primaryOwner }} ({{ assetCheckResult.capability?.primaryPhone }})</span>
          </div>
        </div>

        <el-alert
          v-if="assetCheckResult.capability?.hasRisk"
          type="warning"
          :closable="false"
          show-icon
        >
          存在风险项，请确认后继续
        </el-alert>
      </div>

      <el-empty v-else description="未找到该站点的资产信息" :image-size="80" />

      <template #footer>
        <el-button @click="showAssetCheckDialog = false">取消</el-button>
        <el-button
          v-if="assetCheckResult?.capability?.status !== 'unavailable'"
          type="primary"
          @click="confirmAssetCheck"
        >
          继续创建
        </el-button>
        <el-button
          v-else
          type="primary"
          @click="goToAssetManagement"
        >
          前去管理资产
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { useBarStore } from '@/stores/bar'
import {
  Refresh, Plus, Delete, Loading, Warning, WarningFilled,
  MagicStick, DataAnalysis, List, CircleCheck, CircleClose, User
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ScoreCoverage from '@/components/ai/ScoreCoverage.vue'
import FeaturePlaceholder from '@/components/common/FeaturePlaceholder.vue'
import { aiApi, tendersApi } from '@/api'
import { notifyFeatureUnavailable } from '@/utils/featureFeedback'

const router = useRouter()
const route = useRoute()
const projectStore = useProjectStore()
const userStore = useUserStore()
const barStore = useBarStore()

// 编辑模式
const isEditMode = ref(false)
const editProjectId = ref(null)

const currentStep = ref(0)
const syncing = ref(false)
const syncedFromCRM = ref(false)
const submitting = ref(false)
const analyzing = ref(false)
const showAssetCheckDialog = ref(false)
const assetCheckResult = ref(null)
const scorePreviewPlaceholder = ref(null)
const hasAiStep = true
const lastStepIndex = hasAiStep ? 3 : 2
const availableTenders = ref([])
const selectedTenderId = ref(null)

// 计算页面标题
const pageTitle = computed(() => isEditMode.value ? '编辑项目' : '创建项目')

const userList = computed(() => userStore.users)

// 平台选项（从 BAR store 获取）
const platformOptions = computed(() => barStore.sites || [])

const basicFormRef = ref()
const detailFormRef = ref()
const taskFormRef = ref()

const basicForm = reactive({
  name: '',
  customer: '',
  budget: null,
  industry: '',
  region: '',
  platform: '',
  deadline: '',
  manager: '',
  competitors: []
})

// 常见竞争对手选项
const competitorOptions = ref([
  '华为技术有限公司',
  '腾讯云计算有限公司',
  '阿里巴巴云计算有限公司',
  '百度智能云',
  '京东科技',
  '科大讯飞股份有限公司',
  '浪潮集团有限公司',
  '中软国际',
  '东软集团',
  '用友网络'
])

// 竞争对手分析数据
const competitorAnalysis = ref([])

const detailForm = reactive({
  description: '',
  tags: [],
  startDate: '',
  endDate: '',
  remark: ''
})

const taskForm = reactive({
  tasks: [
    { name: '', owner: '', deadline: '', priority: 'medium', status: 'todo' }
  ]
})

const sourceInfo = reactive({
  module: '',
  customerId: '',
  customerName: '',
  opportunityId: '',
  reasoningSummary: ''
})

// AI分析数据
const aiSummary = ref({
  winScore: 0,
  winLevel: 'low',
  risks: [],
  suggestions: []
})

// 评分分析数据
const scoreAnalysis = ref({
  scoreCategories: [],
  gapItems: []
})

// AI生成的任务清单
const aiGeneratedTasks = ref([])

const basicRules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  customer: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  budget: [{ required: true, message: '请输入预算金额', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择投标截止日期', trigger: 'change' }],
  manager: [{ required: true, message: '请选择项目负责人', trigger: 'change' }]
}

const detailRules = {
  description: [{ required: true, message: '请输入项目描述', trigger: 'blur' }]
}

const formatDateTime = (value, fallbackTime = '00:00:00') => {
  if (!value) return ''
  if (String(value).includes('T')) return String(value)
  return `${value}T${fallbackTime}`
}

const decodeQueryValue = (value) => {
  if (Array.isArray(value)) return decodeQueryValue(value[0])
  if (value === undefined || value === null) return ''
  return String(value)
}

const decodeNumericQuery = (value) => {
  const normalized = decodeQueryValue(value)
  if (!normalized) return null
  const numericValue = Number(normalized)
  return Number.isFinite(numericValue) ? numericValue : null
}

const splitTags = (value) => {
  const normalized = decodeQueryValue(value)
  if (!normalized) return []
  return normalized
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

const applyOpportunityPrefill = () => {
  const projectName = decodeQueryValue(route.query.projectName)
  const customerName = decodeQueryValue(route.query.customerName)
  const industry = decodeQueryValue(route.query.industry)
  const region = decodeQueryValue(route.query.region)
  const predictedBudget = decodeNumericQuery(route.query.budget)
  const deadline = decodeQueryValue(route.query.deadline)
  const description = decodeQueryValue(route.query.description)
  const remark = decodeQueryValue(route.query.remark)
  const tags = splitTags(route.query.tags)

  if (projectName) basicForm.name = projectName
  if (customerName) basicForm.customer = customerName
  if (industry) basicForm.industry = industry
  if (region) basicForm.region = region
  if (predictedBudget !== null) basicForm.budget = predictedBudget
  if (deadline) basicForm.deadline = deadline
  if (description) detailForm.description = description
  if (tags.length > 0) detailForm.tags = tags
  if (remark) detailForm.remark = remark

  sourceInfo.module = decodeQueryValue(route.query.sourceModule)
  sourceInfo.customerId = decodeQueryValue(route.query.sourceCustomerId)
  sourceInfo.customerName = decodeQueryValue(route.query.sourceCustomerName || customerName)
  sourceInfo.opportunityId = decodeQueryValue(route.query.sourceOpportunityId)
  sourceInfo.reasoningSummary = decodeQueryValue(route.query.sourceReasoningSummary)
}

const resolveApiTenderId = () => {
  const routeTenderId = route.query.tenderId
  if (routeTenderId && /^\d+$/.test(String(routeTenderId))) {
    return Number(routeTenderId)
  }
  return selectedTenderId.value || Number(availableTenders.value[0]?.id || 0) || null
}

const buildApiProjectPayload = () => {
  const managerId = Number(userStore.currentUser?.id || 0)
  const tenderId = resolveApiTenderId()
  const startDate = formatDateTime(detailForm.startDate || new Date().toISOString().slice(0, 10))
  const endDate = formatDateTime(detailForm.endDate || basicForm.deadline, '23:59:59')

  if (!managerId) {
    throw new Error('当前登录用户无有效ID，无法创建项目')
  }
  if (!tenderId) {
    throw new Error('当前没有可关联的真实标讯，请先从标讯中心进入或确认 demo tenders 已加载')
  }
  if (!endDate) {
    throw new Error('请填写投标截止日期或预计完工日期')
  }

  return {
    name: basicForm.name,
    tenderId,
    managerId,
    teamMembers: [managerId],
    startDate,
    endDate,
    status: 'INITIATED',
    sourceModule: sourceInfo.module || '',
    sourceCustomerId: sourceInfo.customerId || '',
    sourceCustomer: sourceInfo.customerName || '',
    sourceOpportunityId: sourceInfo.opportunityId || '',
    sourceReasoningSummary: sourceInfo.reasoningSummary || ''
  }
}

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

const syncFromCRM = async () => {
  syncing.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1000))
    basicForm.name = '某央企智慧办公平台采购项目'
    basicForm.customer = '某央企集团'
    basicForm.budget = 500
    basicForm.industry = '政府'
    basicForm.region = '北京'
    syncedFromCRM.value = true
    ElMessage.success('CRM数据同步成功')
  } catch (error) {
    ElMessage.error('CRM数据同步失败')
  } finally {
    syncing.value = false
  }
}

const addTask = () => {
  taskForm.tasks.push({
    name: '',
    owner: '',
    deadline: '',
    priority: 'medium',
    status: 'todo'
  })
}

const removeTask = (index) => {
  taskForm.tasks.splice(index, 1)
}

const nextStep = async () => {
  if (currentStep.value === 0) {
    const valid = await basicFormRef.value?.validate().catch(() => false)
    if (!valid) return
  } else if (currentStep.value === 1) {
    const valid = await detailFormRef.value?.validate().catch(() => false)
    if (!valid) return
  } else if (currentStep.value === 2 && hasAiStep) {
    // 进入智能辅助步骤时，执行AI分析
    await runAIAnalysis()
  }
  currentStep.value++
}

const prevStep = () => {
  currentStep.value--
}

// 运行AI分析
const runAIAnalysis = async () => {
  analyzing.value = true
  try {
    const response = await aiApi.score.generatePreview({
      industry: basicForm.industry,
      tags: detailForm.tags,
      budget: basicForm.budget })

    if (response?.success && response.data) {
      aiSummary.value = response.data.aiSummary
      scoreAnalysis.value = response.data.scoreAnalysis
      aiGeneratedTasks.value = response.data.generatedTasks
      scorePreviewPlaceholder.value = null
      ElMessage.success('AI分析完成')
    } else {
      aiSummary.value = {
        winScore: 0,
        winLevel: 'low',
        risks: [],
        suggestions: [] }
      scoreAnalysis.value = {
        scoreCategories: [],
        gapItems: [] }
      aiGeneratedTasks.value = []
      scorePreviewPlaceholder.value = notifyFeatureUnavailable(response, {
        fallback: {
          title: '评分预览当前不可用',
          hint: '项目创建流程会继续保留，评分建议可在分析服务恢复后补充。' } }) || {
        title: '评分预览不可用',
        message: response?.message || '当前场景未生成评分结果',
        hint: '项目创建流程不受影响。' }
      if (!scorePreviewPlaceholder.value.feature) {
        ElMessage.info(scorePreviewPlaceholder.value.message)
      }
    }
  } catch (error) {
    ElMessage.error('AI分析失败')
  } finally {
    analyzing.value = false
  }
}

// 获取赢面分数样式
const getWinScoreClass = (score) => {
  if (score >= 80) return 'score-high'
  if (score >= 60) return 'score-medium'
  return 'score-low'
}

// 获取赢面等级标签类型
const getWinLevelType = (level) => {
  const typeMap = {
    high: 'success',
    medium: 'warning',
    low: 'danger'
  }
  return typeMap[level] || ''
}

// 获取赢面等级文本
const getWinLevelText = (level) => {
  const textMap = {
    high: '赢面较高',
    medium: '赢面中等',
    low: '赢面较低'
  }
  return textMap[level] || ''
}

// 获取优先级标签类型
const getPriorityType = (priority) => {
  const typeMap = {
    high: 'danger',
    medium: 'warning',
    low: 'info'
  }
  return typeMap[priority] || ''
}

// 获取优先级文本
const getPriorityText = (priority) => {
  const textMap = {
    high: '高',
    medium: '中',
    low: '低'
  }
  return textMap[priority] || ''
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    // 合并用户任务和AI生成的任务
    const userTasks = taskForm.tasks.filter(t => t.name)
    const aiTasks = hasAiStep
      ? aiGeneratedTasks.value
          .filter(t => t.selected)
          .map(t => ({
            name: t.name,
            priority: t.priority,
            status: 'todo',
            owner: basicForm.manager
          }))
      : []

    const projectData = false
      ? {
          ...basicForm,
          ...detailForm,
          sourceModule: sourceInfo.module || '',
          sourceCustomerId: sourceInfo.customerId || '',
          sourceCustomer: sourceInfo.customerName || '',
          sourceOpportunityId: sourceInfo.opportunityId || '',
          sourceReasoningSummary: sourceInfo.reasoningSummary || '',
          tasks: [...userTasks, ...aiTasks],
          competitorAnalysis: competitorAnalysis.value,
          aiAnalysis: hasAiStep
            ? {
                ...aiSummary.value,
                scoreCoverage: scoreAnalysis.value
              }
            : null
        }
      : buildApiProjectPayload()

    const createdProject = await projectStore.createProject(projectData)

    ElMessage.success('项目创建成功')
    if (createdProject?.id) {
      router.push(`/project/${createdProject.id}`)
      return
    }
    router.push('/project')
  } catch (error) {
    ElMessage.error('项目创建失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}

// 处理竞争对手变化
const handleCompetitorsChange = (value) => {
  value.forEach(name => {
    const existing = competitorAnalysis.value.find(c => c.name === name)
    if (!existing) {
      competitorAnalysis.value.push({
        name,
        strength: '',
        weakness: '',
        winRate: 0,
        history: ''
      })
    }
  })
  competitorAnalysis.value = competitorAnalysis.value.filter(
    c => value.includes(c.name)
  )
}

// 处理招标平台变化
const handlePlatformChange = async (platformName) => {
  if (!platformName) return

  // 触发资产检查
  const result = await barStore.checkSiteCapability(platformName)

  if (result.found) {
    assetCheckResult.value = result
    showAssetCheckDialog.value = true
  }
}

// 确认资产检查，继续创建
const confirmAssetCheck = () => {
  showAssetCheckDialog.value = false
  ElMessage.success('已确认资产状态，请继续完善项目信息')
}

// 前往资产管理
const goToAssetManagement = () => {
  showAssetCheckDialog.value = false
  router.push('/resource/bar')
}

// 检测编辑模式并加载项目数据
onMounted(async () => {
  if (!basicForm.manager && userStore.currentUser?.name) {
    basicForm.manager = userStore.currentUser.name
  }

  if (true) {
    const tenderResult = await tendersApi.getList()
    if (tenderResult?.success) {
      availableTenders.value = Array.isArray(tenderResult.data) ? tenderResult.data : []
    }

    if (/^\d+$/.test(String(route.query.tenderId || ''))) {
      selectedTenderId.value = Number(route.query.tenderId)
    } else if (availableTenders.value.length > 0) {
      selectedTenderId.value = Number(availableTenders.value[0].id)
    }
  }

  const editId = route.query.editId
  if (editId) {
    isEditMode.value = true
    editProjectId.value = editId
    await loadProjectData(editId)
  } else {
    applyOpportunityPrefill()
  }
})

// 加载项目数据用于编辑
const loadProjectData = async (id) => {
  try {
    // 从 store 中获取项目数据
    await projectStore.getProjects()
    const project = projectStore.projects.find(p => p.id === id)

    if (project) {
      // 填充基本信息
      basicForm.name = project.name || ''
      basicForm.customer = project.customer || ''
      basicForm.budget = project.budget || null
      basicForm.industry = project.industry || ''
      basicForm.region = project.region || ''
      basicForm.platform = project.platform || ''
      basicForm.deadline = project.deadline || ''
      basicForm.manager = project.manager || ''
      basicForm.competitors = project.competitors || []

      // 填充详细信息
      detailForm.description = project.description || ''
      detailForm.tags = project.tags || []
      detailForm.startDate = project.startDate || ''
      detailForm.endDate = project.endDate || ''
      detailForm.remark = project.remark || ''

      // 填充任务信息
      if (project.tasks && project.tasks.length > 0) {
        taskForm.tasks = project.tasks
      }

      ElMessage.success('项目数据加载成功')
    } else {
      ElMessage.error('未找到该项目')
      // 返回列表页
      router.push('/project')
    }
  } catch (error) {
    console.error('加载项目数据失败:', error)
    ElMessage.error('加载项目数据失败')
  }
}
</script>

<style scoped>
.project-create-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.steps {
  margin: 32px 0;
  padding: 0 40px;
}

.step-content {
  min-height: 400px;
  padding: 20px 40px;
}

.step-panel {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.mb-16 {
  margin-bottom: 16px;
}

.mb-12 {
  margin-bottom: 12px;
}

.mt-16 {
  margin-top: 16px;
}

.sync-tip {
  margin-left: 12px;
  color: #67c23a;
  font-size: 14px;
}

.competitor-analysis-item {
  display: block;
}

.competitor-analysis-item :deep(.el-form-item__content) {
  width: 100%;
}

.win-rate-input {
  display: flex;
  align-items: center;
  gap: 4px;
}

.task-list {
  margin-bottom: 16px;
}

.task-item {
  margin-bottom: 16px;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.add-task-btn {
  width: 100%;
  border-style: dashed;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  margin-top: 20px;
}

/* AI分析相关样式 */
.ai-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.ai-summary {
  width: 100%;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.win-score {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.score-label {
  font-size: 14px;
  color: #606266;
}

.score-value {
  font-size: 32px;
  font-weight: 600;
}

.score-high {
  color: #67c23a;
}

.score-medium {
  color: #e6a23c;
}

.score-low {
  color: #f56c6c;
}

.score-max {
  font-size: 16px;
  color: #909399;
}

.win-level {
  flex-shrink: 0;
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.risk-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: #fef0f0;
  border-radius: 8px;
  border-left: 4px solid #f56c6c;
}

.risk-item.risk-medium {
  background-color: #fdf6ec;
  border-left-color: #e6a23c;
}

.risk-icon {
  font-size: 20px;
}

.risk-item.risk-high .risk-icon {
  color: #f56c6c;
}

.risk-item.risk-medium .risk-icon {
  color: #e6a23c;
}

.risk-content {
  flex: 1;
  color: #606266;
}

.suggestion-list {
  margin: 0;
  padding-left: 20px;
  margin-bottom: 16px;
}

.suggestion-list li {
  margin-bottom: 8px;
  color: #606266;
  line-height: 1.6;
}

.ai-tasks {
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.ai-task-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background-color: #fff;
  border-radius: 6px;
  margin-bottom: 8px;
}

.ai-task-item:last-child {
  margin-bottom: 0;
}

.ai-task-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-left: 24px;
}

.ai-task-suggest {
  font-size: 12px;
  color: #909399;
}

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .project-create-container {
    padding: 12px;
  }

  .page-header {
    margin-bottom: 12px;
  }

  .page-title {
    font-size: 20px;
  }

  :deep(.el-steps) {
    font-size: 12px;
  }

  :deep(.el-step__title) {
    font-size: 12px;
  }

  .create-form :deep(.el-form-item__label) {
    width: 100% !important;
    text-align: left;
  }

  .create-form :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  .create-form :deep(.el-input),
  .create-form :deep(.el-select),
  .create-form :deep(.el-date-picker) {
    width: 100% !important;
  }

  .step-actions {
    flex-wrap: wrap;
    gap: 12px;
  }

  .step-actions .el-button {
    flex: 1;
    min-width: 100px;
  }

  .task-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  :deep(.el-drawer) {
    width: 95% !important;
  }

  :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }

  :deep(.el-dialog__body) {
    padding: 16px;
  }

  .create-card {
    margin-bottom: 12px;
  }

  .create-card :deep(.el-card__header) {
    padding: 12px 16px;
  }

  .create-card :deep(.el-card__body) {
    padding: 16px;
  }

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100% !important;
    min-height: 120px;
  }

  .summary-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .risk-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .ai-task-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    padding-left: 0;
  }
}

@media (hover: none) and (pointer: coarse) {
  .step-actions .el-button,
  .add-task-btn {
    min-height: 44px;
  }

  .task-item {
    min-height: 60px;
  }

  .task-item:active {
    background: #f5f7fa;
  }
}

/* 资产检查弹窗样式 */
.asset-check-result {
  padding: 16px 0;
}

.asset-check-result .check-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.asset-check-result .site-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.asset-check-result .check-items {
  margin-bottom: 20px;
}

.asset-check-result .check-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  font-size: 15px;
}

.asset-check-result .check-item .icon-success {
  color: #67c23a;
  font-size: 22px;
}

.asset-check-result .check-item .icon-error {
  color: #f56c6c;
  font-size: 22px;
}

.asset-check-result .check-item .icon-user {
  color: #909399;
  font-size: 20px;
}
</style>
