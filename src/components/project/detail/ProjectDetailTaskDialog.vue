<template>
  <el-dialog v-model="detail.taskDialogVisible.value" title="任务详情" width="500px">
    <el-descriptions v-if="detail.currentTask.value" :column="1" border>
      <el-descriptions-item label="任务名称">{{ detail.currentTask.value.name }}</el-descriptions-item>
      <el-descriptions-item label="负责人">{{ detail.currentTask.value.owner }}</el-descriptions-item>
      <el-descriptions-item label="截止日期">{{ detail.currentTask.value.deadline }}</el-descriptions-item>
      <el-descriptions-item label="优先级"><el-tag :type="detail.getPriorityType(detail.currentTask.value.priority)">{{ detail.getPriorityText(detail.currentTask.value.priority) }}</el-tag></el-descriptions-item>
      <el-descriptions-item label="状态"><el-tag :type="detail.getTaskStatusType(detail.currentTask.value.status)">{{ detail.getTaskStatusText(detail.currentTask.value.status) }}</el-tag></el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup>
import { inject } from 'vue'
import { projectDetailKey } from '@/composables/projectDetail/context.js'

const detail = inject(projectDetailKey)
</script>
