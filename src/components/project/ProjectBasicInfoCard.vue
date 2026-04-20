<template>
  <el-card class="info-card">
    <template #header>
      <div class="card-title">
        <el-icon><InfoFilled /></el-icon>
        <span>项目信息</span>
      </div>
    </template>

    <el-descriptions :column="2" border>
      <el-descriptions-item label="项目名称">{{ project?.name }}</el-descriptions-item>
      <el-descriptions-item label="客户">{{ project?.customer }}</el-descriptions-item>
      <el-descriptions-item label="预算">{{ project?.budget }} 万元</el-descriptions-item>
      <el-descriptions-item label="行业">{{ project?.industry }}</el-descriptions-item>
      <el-descriptions-item label="地区">{{ project?.region }}</el-descriptions-item>
      <el-descriptions-item label="负责人">{{ project?.manager }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ project?.createTime }}</el-descriptions-item>
      <el-descriptions-item label="截止日期">{{ project?.deadline }}</el-descriptions-item>
      <el-descriptions-item label="项目描述" :span="2">
        {{ project?.description }}
      </el-descriptions-item>
    </el-descriptions>

    <div class="progress-section">
      <div class="progress-header">
        <span class="progress-label">项目进度</span>
        <span class="progress-value">{{ formatScore(project?.progress || 0) }}%</span>
      </div>
      <el-progress
        :percentage="Number(project?.progress || 0)"
        :status="getProgressStatus(project?.progress)"
        :stroke-width="20"
      />
    </div>
  </el-card>
</template>

<script setup>
import { InfoFilled } from '@element-plus/icons-vue'

defineProps({
  project: {
    type: Object,
    default: null,
  },
})

function formatScore(score) {
  return Number(score).toFixed(2)
}

function getProgressStatus(progress) {
  if (progress === 100) return 'success'
  return undefined
}
</script>

<style scoped>
.info-card {
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.progress-section {
  margin-top: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.progress-label {
  font-size: 14px;
  color: #606266;
}

.progress-value {
  font-size: 16px;
  font-weight: 500;
  color: #409eff;
}
</style>
