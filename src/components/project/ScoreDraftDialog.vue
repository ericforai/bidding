<template>
  <el-dialog
    :model-value="modelValue"
    title="评分标准拆解确认"
    width="1100px"
    top="5vh"
    @close="handleClose"
  >
    <div class="score-draft-dialog">
      <div class="summary-grid">
        <el-card shadow="never">
          <div class="summary-label">总评分项</div>
          <div class="summary-value">{{ drafts.length }}</div>
        </el-card>
        <el-card shadow="never">
          <div class="summary-label">待确认</div>
          <div class="summary-value">{{ counts.draft }}</div>
        </el-card>
        <el-card shadow="never">
          <div class="summary-label">可生成</div>
          <div class="summary-value">{{ counts.ready }}</div>
        </el-card>
        <el-card shadow="never">
          <div class="summary-label">暂不生成</div>
          <div class="summary-value">{{ counts.skipped }}</div>
        </el-card>
      </div>

      <div class="toolbar">
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept=".doc,.docx"
          :on-change="handleFileChange"
        >
          <el-button type="primary">选择评分文件</el-button>
        </el-upload>
        <span class="selected-file">{{ selectedFile?.name || '未选择文件' }}</span>
        <el-button type="success" :disabled="!selectedFile" :loading="parsing" @click="handleParse">
          解析评分表
        </el-button>
        <el-button :disabled="!drafts.length" @click="handleReload">刷新草稿</el-button>
        <el-button :disabled="!drafts.length" @click="handleClear">清空草稿</el-button>
      </div>

      <div class="batch-toolbar" v-if="drafts.length">
        <el-select v-model="batchAssigneeId" placeholder="批量指定责任人" clearable style="width: 220px;">
          <el-option
            v-for="user in users"
            :key="user.id"
            :label="`${user.name} (${user.dept || user.role || '未分组'})`"
            :value="user.id"
          />
        </el-select>
        <el-date-picker
          v-model="batchDueDate"
          type="date"
          placeholder="批量设置截止日期"
          value-format="YYYY-MM-DD"
        />
        <el-button :disabled="!selectedRows.length || !batchAssigneeId" @click="applyBatchAssignee">批量指派</el-button>
        <el-button :disabled="!selectedRows.length || !batchDueDate" @click="applyBatchDueDate">批量设置截止</el-button>
        <el-button :disabled="!selectedRows.length" @click="markSelectedSkipped">标记暂不生成</el-button>
      </div>

      <el-table
        v-if="drafts.length"
        :data="drafts"
        border
        height="460"
        @selection-change="selectedRows = $event"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column label="类别" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="categoryTagType(row.category)">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scoreItemTitle" label="评分项" min-width="180" />
        <el-table-column label="评分标准" min-width="260">
          <template #default="{ row }">
            <div class="rule-text">{{ row.scoreRuleText }}</div>
            <div class="score-meta">{{ row.scoreValueText || '未标注分值' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="任务标题" min-width="220">
          <template #default="{ row }">
            <el-input
              v-model="row.generatedTaskTitle"
              @blur="persistDraft(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="建议交付物" min-width="180">
          <template #default="{ row }">
            <div class="deliverables">
              <el-tag v-for="item in row.suggestedDeliverables" :key="item" size="small">{{ item }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="责任人" width="180">
          <template #default="{ row }">
            <el-select
              v-model="row.assigneeId"
              placeholder="请选择"
              clearable
              @change="handleAssigneeChange(row, $event)"
            >
              <el-option
                v-for="user in users"
                :key="user.id"
                :label="user.name"
                :value="user.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="截止日期" width="150">
          <template #default="{ row }">
            <el-date-picker
              v-model="row.dueDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="截止日期"
              @change="persistDraft(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="warning" @click="toggleSkip(row)">
              {{ row.status === 'SKIPPED' ? '恢复' : '暂不生成' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="先选择评分文件并解析评分表" />
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
      <el-button
        type="primary"
        :disabled="readyDraftIds.length === 0"
        :loading="generating"
        @click="handleGenerate"
      >
        生成正式任务（{{ readyDraftIds.length }}）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { projectsApi } from '@/api'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  projectId: {
    type: [String, Number],
    required: true,
  },
  users: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'tasks-generated'])

const drafts = ref([])
const selectedRows = ref([])
const selectedFile = ref(null)
const parsing = ref(false)
const generating = ref(false)
const batchAssigneeId = ref('')
const batchDueDate = ref('')

const counts = computed(() => ({
  draft: drafts.value.filter((item) => item.status === 'DRAFT').length,
  ready: drafts.value.filter((item) => item.status === 'READY').length,
  skipped: drafts.value.filter((item) => item.status === 'SKIPPED').length,
}))

const readyDraftIds = computed(() => drafts.value
  .filter((item) => item.status === 'READY')
  .map((item) => item.id))

watch(() => props.modelValue, async (visible) => {
  if (visible) {
    await handleReload()
  }
})

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleParse = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择评分文件')
    return
  }
  parsing.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const result = await projectsApi.parseScoreDrafts(props.projectId, formData)
    drafts.value = Array.isArray(result?.data?.drafts) ? result.data.drafts : []
    selectedRows.value = []
    ElMessage.success(`已解析 ${drafts.value.length} 条评分草稿`)
  } catch (error) {
    ElMessage.error(error.message || '评分表解析失败')
  } finally {
    parsing.value = false
  }
}

const handleReload = async () => {
  try {
    const result = await projectsApi.getScoreDrafts(props.projectId)
    drafts.value = Array.isArray(result?.data) ? result.data : []
  } catch (error) {
    ElMessage.error(error.message || '加载评分草稿失败')
  }
}

const handleClear = async () => {
  await ElMessageBox.confirm('确认清空当前项目的评分草稿？', '清空确认', {
    type: 'warning',
    confirmButtonText: '确认清空',
    cancelButtonText: '取消',
  })
  await projectsApi.clearScoreDrafts(props.projectId)
  drafts.value = []
  selectedRows.value = []
  ElMessage.success('评分草稿已清空')
}

const persistDraft = async (row) => {
  const payload = {
    assigneeId: row.assigneeId || null,
    assigneeName: row.assigneeName || '',
    dueDate: normalizeDueDate(row.dueDate),
    generatedTaskTitle: row.generatedTaskTitle,
    generatedTaskDescription: row.generatedTaskDescription,
    status: row.status,
    skipReason: row.skipReason || '',
  }
  const result = await projectsApi.updateScoreDraft(props.projectId, row.id, payload)
  if (result?.success && result.data) {
    Object.assign(row, result.data)
  }
}

const handleAssigneeChange = async (row, assigneeId) => {
  const user = props.users.find((item) => String(item.id) === String(assigneeId))
  row.assigneeName = user?.name || ''
  row.status = assigneeId ? 'READY' : 'DRAFT'
  await persistDraft(row)
}

const applyBatchAssignee = async () => {
  const user = props.users.find((item) => String(item.id) === String(batchAssigneeId.value))
  if (!user) return
  for (const row of selectedRows.value) {
    row.assigneeId = user.id
    row.assigneeName = user.name
    row.status = 'READY'
    await persistDraft(row)
  }
  ElMessage.success(`已批量指派给 ${user.name}`)
}

const applyBatchDueDate = async () => {
  for (const row of selectedRows.value) {
    row.dueDate = batchDueDate.value
    await persistDraft(row)
  }
  ElMessage.success('已批量设置截止日期')
}

const markSelectedSkipped = async () => {
  for (const row of selectedRows.value) {
    row.status = 'SKIPPED'
    row.skipReason = '人工暂不生成'
    await persistDraft(row)
  }
  ElMessage.success('已标记为暂不生成')
}

const toggleSkip = async (row) => {
  if (row.status === 'SKIPPED') {
    row.status = row.assigneeId || row.assigneeName ? 'READY' : 'DRAFT'
    row.skipReason = ''
  } else {
    row.status = 'SKIPPED'
    row.skipReason = '人工暂不生成'
  }
  await persistDraft(row)
}

const handleGenerate = async () => {
  generating.value = true
  try {
    const result = await projectsApi.generateScoreDraftTasks(props.projectId, readyDraftIds.value)
    const tasks = Array.isArray(result?.data) ? result.data : []
    await handleReload()
    emit('tasks-generated', tasks)
    ElMessage.success(`已生成 ${tasks.length} 条正式任务`)
  } catch (error) {
    ElMessage.error(error.message || '生成正式任务失败')
  } finally {
    generating.value = false
  }
}

const normalizeDueDate = (value) => {
  if (!value) return null
  return value.includes('T') ? value : `${value}T18:00:00`
}

const categoryLabel = (value) => ({
  technical: '技术',
  business: '商务',
  price: '价格',
  other: '其他',
}[value] || '其他')

const categoryTagType = (value) => ({
  technical: 'success',
  business: 'warning',
  price: 'danger',
  other: 'info',
}[value] || 'info')

const statusLabel = (value) => ({
  DRAFT: '草稿',
  READY: '待生成',
  SKIPPED: '已跳过',
  GENERATED: '已生成',
}[value] || value)

const statusTagType = (value) => ({
  DRAFT: 'info',
  READY: 'success',
  SKIPPED: 'warning',
  GENERATED: '',
}[value] || 'info')
</script>

<style scoped>
.score-draft-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-label {
  font-size: 12px;
  color: #909399;
}

.summary-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.toolbar,
.batch-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.selected-file {
  color: #606266;
  font-size: 13px;
}

.rule-text {
  line-height: 1.5;
  color: #303133;
}

.score-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.deliverables {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
</style>
