<template>
  <el-card class="stage-view" shadow="never">
    <template #header>结果确认 (Result)</template>
    <el-form :model="form" label-width="140px">
      <el-form-item label="结果类型" required>
        <el-radio-group v-model="form.resultType">
          <el-radio label="WON">中标</el-radio>
          <el-radio label="LOST">未中标</el-radio>
          <el-radio label="FAILED">流标</el-radio>
          <el-radio label="ABANDONED">弃标</el-radio>
        </el-radio-group>
      </el-form-item>
      <template v-if="form.resultType === 'WON'">
        <el-form-item label="中标金额(万元)">
          <el-input-number v-model="form.awardAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="合同开始日期">
          <el-date-picker v-model="form.contractStartDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="合同结束日期">
          <el-date-picker v-model="form.contractEndDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
      </template>
      <el-form-item label="结果摘要" v-if="form.resultType === 'FAILED' || form.resultType === 'ABANDONED'" required>
        <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请填写流标/弃标原因摘要..." />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.notes" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="凭证文件" required>
        <el-upload
          v-model:file-list="evidenceFiles"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :accept="acceptedTypes"
          :before-upload="beforeUpload"
          multiple
          :limit="5"
          :on-success="handleUploadSuccess"
          :on-remove="handleUploadRemove"
        >
          <el-button type="primary">上传凭证</el-button>
          <template #tip>
            <div class="el-upload__tip">支持 PDF/图片格式（PDF/JPG/PNG），单文件不超过 10MB，最多 5 个</div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">登记结果</el-button>
      </el-form-item>
    </el-form>
    <el-divider v-if="existing" />
    <el-descriptions v-if="existing" :column="2" border size="small">
      <el-descriptions-item label="已登记类型">{{ existing.resultType }}</el-descriptions-item>
      <el-descriptions-item label="登记时间">{{ existing.registeredAt }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectLifecycleApi } from '@/api/modules/projectLifecycle.js'
import { useUserStore } from '@/stores/user.js'

const props = defineProps({ projectId: { type: [String, Number], required: true } })
const emit = defineEmits(['registered'])
const userStore = useUserStore()

const form = reactive({
  resultType: 'WON',
  awardAmount: 0,
  contractStartDate: '',
  contractEndDate: '',
  notes: '',
  summary: '',
  evidenceFileIds: [],
})
const evidenceFiles = ref([])
const existing = ref(null)
const submitting = ref(false)

// 文件上传配置
const uploadUrl = '/api/upload'
const acceptedTypes = '.pdf,.jpg,.jpeg,.png'
const MAX_FILE_SIZE_MB = 10
const ALLOWED_MIMES = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png']

const uploadHeaders = computed(() => {
  const token = userStore?.token
  return token ? { Authorization: `Bearer ${token}` } : {}
})

function beforeUpload(file) {
  if (!ALLOWED_MIMES.includes(file.type)) {
    ElMessage.error(`不支持的文件类型: ${file.type || '未知'}`)
    return false
  }
  if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
    ElMessage.error(`文件不能超过 ${MAX_FILE_SIZE_MB}MB`)
    return false
  }
  return true
}

function handleUploadSuccess(response) {
  if (response?.data?.id) {
    form.evidenceFileIds.push(response.data.id)
  } else {
    ElMessage.warning('上传响应异常，缺少文件ID')
  }
}

function handleUploadRemove(uploadFile) {
  const idx = form.evidenceFileIds.indexOf(uploadFile.response?.data?.id)
  if (idx > -1) form.evidenceFileIds.splice(idx, 1)
}

async function load() {
  try {
    const r = await projectLifecycleApi.getResult(props.projectId)
    existing.value = r?.data || r
  } catch (e) {
    if (e?.response?.status !== 404) console.warn(e)
  }
}

async function submit() {
  if (!form.resultType) return ElMessage.warning('请选择结果类型')
  if ((form.resultType === 'FAILED' || form.resultType === 'ABANDONED') && !form.summary?.trim()) {
    return ElMessage.warning('流标/弃标结果需填写摘要')
  }
  if (!form.evidenceFileIds.length) return ElMessage.warning('请上传凭证文件')
  submitting.value = true
  try {
    const payload = {
      resultType: form.resultType,
      awardAmount: form.resultType === 'WON' ? form.awardAmount : null,
      contractStartDate: form.resultType === 'WON' ? form.contractStartDate || null : null,
      contractEndDate: form.resultType === 'WON' ? form.contractEndDate || null : null,
      notes: form.notes,
      summary: form.summary,
      evidenceFileIds: form.evidenceFileIds,
    }
    await projectLifecycleApi.registerResult(props.projectId, payload)
    ElMessage.success('结果已登记')
    emit('registered')
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '登记失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
