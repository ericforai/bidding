<template>
  <div class="task-form">
    <el-form :model="localValue" label-width="110px" :disabled="readonly">
      <el-form-item label="任务名称" required>
        <el-input v-model="localValue.name" placeholder="请输入任务名称" />
      </el-form-item>

      <el-form-item label="详细描述">
        <el-input
          v-model="localValue.content"
          type="textarea"
          :rows="6"
          placeholder="支持 Markdown：# 标题、- 列表、**加粗** 等"
        />
      </el-form-item>

      <el-form-item label="负责人">
        <el-select
          v-model="localValue.assigneeId"
          data-test="task-owner-select"
          filterable
          style="width: 100%"
          placeholder="请选择负责人"
          :loading="loadingAssignees"
          @change="handleAssigneeChange"
        >
          <el-option
            v-for="person in assigneeOptions"
            :key="person.userId"
            :label="assigneeLabel(person)"
            :value="person.userId"
          >
            <div class="assignee-option">
              <span>{{ person.name }}</span>
              <small>{{ person.deptName || '未配置部门' }} · {{ person.roleName || '未配置角色' }}</small>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="截止日期">
        <el-date-picker v-model="localValue.deadline" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>

      <el-form-item label="优先级">
        <el-select v-model="localValue.priority" style="width: 100%">
          <el-option label="高" value="high" />
          <el-option label="中" value="medium" />
          <el-option label="低" value="low" />
        </el-select>
      </el-form-item>

      <el-form-item label="状态">
        <el-select v-model="localValue.status" style="width: 100%" :loading="loadingStatuses">
          <el-option v-for="s in statuses" :key="s.code" :label="s.name" :value="s.code" />
        </el-select>
      </el-form-item>

      <el-alert v-if="validationMessage" type="warning" :closable="false" :title="validationMessage" />
    </el-form>

    <template v-if="extendedFieldSchema.length > 0">
      <el-divider>扩展字段</el-divider>
      <DynamicFormRenderer
        ref="extFormRef"
        :fields="extendedFieldSchema"
        v-model="localValue.extendedFields"
        :disabled="readonly"
      />
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch, onMounted } from 'vue'
import { taskStatusDictApi } from '@/api/modules/taskStatusDict.js'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import DynamicFormRenderer from '@/components/common/DynamicFormRenderer.vue'
import { useTaskAssigneeOptions } from './useTaskAssigneeOptions.js'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) },
  mode: { type: String, default: 'create' }, // create | edit | view
})
const emit = defineEmits(['submit', 'update:modelValue'])

const projectStore = useProjectStore()
const userStore = useUserStore()

const localValue = reactive({ ...props.modelValue })
if (!localValue.extendedFields) {
  localValue.extendedFields = {}
}
const statuses = ref([])
const loadingStatuses = ref(false)
const validationMessage = ref('')
const extFormRef = ref(null)
const readonly = computed(() => props.mode === 'view')
let syncingFromModel = false
const { assigneeOptions, loadingAssignees, loadAssignees, ensureSelectedAssignee, handleAssigneeChange, assigneeLabel } =
  useTaskAssigneeOptions({ localValue, isCreateMode: () => props.mode === 'create', userStore })

const extendedFieldSchema = computed(() =>
  (projectStore.taskExtendedFields || []).map((f) => ({
    key: f.key,
    label: f.label,
    type: f.fieldType, // already lowercase from backend
    required: f.required,
    placeholder: f.placeholder,
    options: f.options, // already parsed array
  }))
)

watch(() => props.modelValue, (v) => {
  syncingFromModel = true
  Object.keys(localValue).forEach((k) => delete localValue[k])
  Object.assign(localValue, v || {})
  if (!localValue.extendedFields) {
    localValue.extendedFields = {}
  }
  ensureSelectedAssignee()
  nextTick(() => {
    syncingFromModel = false
  })
})

watch(localValue, () => {
  if (!syncingFromModel) {
    emit('update:modelValue', { ...localValue })
  }
}, { deep: true })

onMounted(() => {
  projectStore.loadTaskExtendedFields()
  ensureSelectedAssignee()
  setTimeout(() => {
    loadAssignees()
  }, 0)
  loadStatuses()
})

async function loadStatuses() {
  loadingStatuses.value = true
  try {
    const res = await taskStatusDictApi.list()
    statuses.value = res?.data || []
    if (!localValue.status && statuses.value.length > 0) {
      const initialStatus = statuses.value.find((s) => s.initial) || statuses.value[0]
      localValue.status = initialStatus.code
    }
  } catch (err) {
    console.error('[TaskForm] Failed to load task status dict', err)
  } finally {
    loadingStatuses.value = false
  }
}

function validate() {
  if (!localValue.name || !String(localValue.name).trim()) {
    validationMessage.value = '请填写任务名称'
    return validationMessage.value
  }
  validationMessage.value = ''
  return ''
}

function submit() {
  const msg = validate()
  if (msg) return { valid: false, message: msg }
  // Extended fields — if any, validate via DynamicFormRenderer
  if (extendedFieldSchema.value.length > 0) {
    const extRes = extFormRef.value?.submit?.()
    if (extRes && extRes.valid === false) {
      return extRes // propagate {valid, message} from extended form
    }
  }
  emit('submit', { ...localValue })
  return { valid: true, data: { ...localValue } }
}

defineExpose({ submit, validate })
</script>

<style scoped>
.task-form { width: 100%; }

.assignee-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assignee-option small {
  color: #909399;
  font-size: 12px;
}
</style>
