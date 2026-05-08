<template>
  <el-card class="stage-view" shadow="never">
    <template #header>评标 (Evaluation)</template>
    <el-descriptions :column="2" border size="small">
      <el-descriptions-item label="当前子阶段">{{ view?.subStage || '-' }}</el-descriptions-item>
      <el-descriptions-item label="结果是否已公示">{{ view?.announced ? '是' : '否' }}</el-descriptions-item>
    </el-descriptions>
    <el-divider />
    <div class="row">
      <el-select v-model="targetSubStage" placeholder="切换至子阶段" style="width: 220px">
        <el-option label="评标进行中" value="IN_PROGRESS" />
        <el-option label="待定标" value="AWAITING_BOARD" />
        <el-option label="结果已公示" value="ANNOUNCED" />
      </el-select>
      <el-button type="primary" :loading="transitioning" @click="transition">切换子阶段</el-button>
    </div>
    <el-divider />
    <div class="row">
      <el-input
        v-model.number="evidenceDocumentId"
        type="number"
        placeholder="凭证文档 ID"
        style="width: 220px"
      />
      <el-button :loading="attaching" @click="attachEvidence">附加证据</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectLifecycleApi } from '@/api/modules/projectLifecycle.js'

const props = defineProps({ projectId: { type: [String, Number], required: true } })

const view = ref(null)
const targetSubStage = ref('')
const transitioning = ref(false)
const evidenceDocumentId = ref(null)
const attaching = ref(false)

async function load() {
  try {
    const r = await projectLifecycleApi.getEvaluation(props.projectId)
    view.value = r?.data || r
  } catch (e) {
    console.warn(e)
  }
}

async function transition() {
  if (!targetSubStage.value) return ElMessage.warning('请选择子阶段')
  transitioning.value = true
  try {
    await projectLifecycleApi.transitionEvaluationSubStage(props.projectId, {
      target: targetSubStage.value,
    })
    ElMessage.success('子阶段已切换')
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '切换失败')
  } finally {
    transitioning.value = false
  }
}

async function attachEvidence() {
  if (!evidenceDocumentId.value) return ElMessage.warning('请输入文档 ID')
  attaching.value = true
  try {
    await projectLifecycleApi.attachEvaluationEvidence(props.projectId, {
      documentId: evidenceDocumentId.value,
    })
    ElMessage.success('证据已附加')
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '附加失败')
  } finally {
    attaching.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.row { display: flex; gap: 12px; align-items: center; }
</style>
