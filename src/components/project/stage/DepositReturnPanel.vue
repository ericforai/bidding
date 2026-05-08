<template>
  <el-card class="deposit-return-panel" shadow="never">
    <template #header>
      <div class="header">
        <span>保证金信息</span>
        <el-tag v-if="hasDeposit" :type="statusTagType" size="small">{{ statusText }}</el-tag>
        <el-tag v-else type="info" size="small">无保证金</el-tag>
      </div>
    </template>
    <el-descriptions :column="2" border size="small">
      <el-descriptions-item label="是否需要保证金">{{ hasDeposit ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="保证金金额">{{ formatAmount(preview?.depositAmount) }}</el-descriptions-item>
      <el-descriptions-item label="退回状态">{{ statusText }}</el-descriptions-item>
      <el-descriptions-item label="退回日期">{{ preview?.depositReturnDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="退回凭证 ID" :span="2">
        {{ preview?.depositReturnEvidenceId || '-' }}
      </el-descriptions-item>
    </el-descriptions>
    <div v-if="(preview?.blockingReasons || []).length" class="blocking">
      <el-alert
        v-for="(reason, idx) in preview.blockingReasons"
        :key="idx"
        :title="reason"
        type="warning"
        :closable="false"
        show-icon
      />
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  preview: { type: Object, default: () => null },
})

const hasDeposit = computed(() => !!props.preview?.hasDeposit)
const statusText = computed(() => {
  const s = props.preview?.depositReturnStatus
  if (s === 'RETURNED') return '已退回'
  if (s === 'NOT_RETURNED') return '未退回'
  if (s === 'NA') return '不适用'
  return '-'
})
const statusTagType = computed(() => {
  const s = props.preview?.depositReturnStatus
  if (s === 'RETURNED') return 'success'
  if (s === 'NOT_RETURNED') return 'danger'
  return 'info'
})

function formatAmount(v) {
  if (v === null || v === undefined || v === '') return '-'
  const num = Number(v)
  return Number.isFinite(num) ? num.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : String(v)
}
</script>

<style scoped>
.deposit-return-panel .header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.blocking {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
