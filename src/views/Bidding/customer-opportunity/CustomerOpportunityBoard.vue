<template>
  <template v-if="loading">
    <div class="top-board">
      <el-skeleton v-for="i in 4" :key="i" animated>
        <template #template>
          <div class="board-card skeleton-card">
            <el-skeleton-item variant="text" style="width: 50%" />
            <el-skeleton-item variant="h3" style="width: 80%; margin-top: 12px" />
            <el-skeleton-item variant="text" style="width: 60%; margin-top: 8px" />
          </div>
        </template>
      </el-skeleton>
    </div>
  </template>

  <div v-else class="top-board">
    <div class="board-card hover-lift" v-for="item in boardSummaries" :key="item.label">
      <div class="card-label">
        <span>{{ item.label }}</span>
        <el-tag size="small" :type="item.tagType" effect="light" class="tag-glow">{{ item.tag }}</el-tag>
      </div>
      <div class="card-main">
        <div class="card-value">{{ item.value }}</div>
        <div class="card-trend" :class="item.placeholder ? 'neutral' : (item.isUp ? 'up' : 'down')">
          <template v-if="item.placeholder">
            <el-tag size="small" type="info" effect="plain">{{ item.trendLabel || '未接入' }}</el-tag>
          </template>
          <template v-else>
            <el-icon><CaretTop v-if="item.isUp" /><CaretBottom v-else /></el-icon>
            {{ item.trend }}%
          </template>
        </div>
      </div>
      <div class="spark-box">
        <div class="spark-line" :class="item.tagType"></div>
      </div>
      <p class="card-note">{{ item.note }}</p>
    </div>
  </div>
</template>

<script setup>
import { CaretBottom, CaretTop } from '@element-plus/icons-vue'

defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  boardSummaries: {
    type: Array,
    default: () => []
  }
})
</script>
