<template>
  <el-card class="stage-view" shadow="never">
    <template #header>结果确认 (Result)</template>
    <el-form :model="form" label-width="140px">
      <el-form-item label="结果类型" required>
        <el-radio-group v-model="form.resultType">
          <el-radio label="WON">中标</el-radio>
          <el-radio label="LOST">未中标</el-radio>
          <el-radio label="VOID">流标</el-radio>
          <el-radio label="WITHDRAWN">弃标</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="中标金额(万元)" v-if="form.resultType === 'WON'">
        <el-input-number v-model="form.bidAmount" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.notes" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="证据文档 ID">
        <el-input-number v-model="form.evidenceDocumentId" :min="1" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectLifecycleApi } from '@/api/modules/projectLifecycle.js'

const props = defineProps({ projectId: { type: [String, Number], required: true } })
const emit = defineEmits(['registered'])

const form = reactive({
  resultType: 'WON',
  bidAmount: 0,
  notes: '',
  evidenceDocumentId: null,
})
const existing = ref(null)
const submitting = ref(false)

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
  submitting.value = true
  try {
    await projectLifecycleApi.registerResult(props.projectId, form)
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
