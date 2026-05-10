<template>
  <div class="evaluation-status">
    <el-card shadow="never" class="status-card">
      <template #header>
        <span>评估状态信息</span>
      </template>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="当前子阶段">
          {{ view?.subStage || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="评标开始时间">
          {{ formatDate(view?.evaluationStartedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="收到定标材料时间">
          {{ formatDate(view?.boardReceivedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="公示时间">
          {{ formatDate(view?.announcedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="建议是否投标">
          {{ formatRecommendation(view?.recommendation) }}
        </el-descriptions-item>
        <el-descriptions-item label="最后更新人">
          {{ view?.updatedBy || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="最后更新时间">
          {{ formatDate(view?.updatedAt) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="status-card" style="margin-top: 16px">
      <template #header>
        <span>子阶段切换</span>
      </template>
      <el-form label-width="100px" size="small">
        <el-form-item label="切换至">
          <el-select v-model="targetSubStage" placeholder="选择子阶段" style="width: 100%">
            <el-option label="评标进行中" value="IN_PROGRESS" />
            <el-option label="待定标" value="AWAITING_BOARD" />
            <el-option label="结果已公示" value="ANNOUNCED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" :loading="transitioning" @click="$emit('transition', targetSubStage)">
            确认切换
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
defineProps({
  view: { type: Object, default: null },
  transitioning: { type: Boolean, default: false },
  targetSubStage: { type: String, default: '' }
})

defineEmits(['transition'])

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatRecommendation(val) {
  if (val === true) return '建议投标'
  if (val === false) return '不建议投标'
  return '-'
}
</script>

<style scoped>
.evaluation-status {
  width: 320px;
  flex-shrink: 0;
}

.status-card {
  border: 1px solid #ebeef5;
}
</style>
