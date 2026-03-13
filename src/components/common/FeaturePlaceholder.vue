<template>
  <div class="feature-placeholder" :class="{ compact }">
    <el-empty :description="title">
      <template #image>
        <el-icon :size="compact ? 36 : 48" class="placeholder-icon"><WarningFilled /></el-icon>
      </template>
      <p class="placeholder-message">{{ message }}</p>
      <p v-if="hint" class="placeholder-hint">{{ hint }}</p>
    </el-empty>
  </div>
</template>

<script setup>
import { WarningFilled } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    default: '功能暂未接入',
  },
  message: {
    type: String,
    default: '后端暂未提供该能力',
  },
  hint: {
    type: String,
    default: '',
  },
  compact: {
    type: Boolean,
    default: false,
  },
})
</script>

<style scoped>
.feature-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  padding: 16px;
  border: 1px dashed #d0d7de;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.feature-placeholder.compact {
  min-height: 220px;
}

.placeholder-icon {
  color: #e6a23c;
}

.placeholder-message,
.placeholder-hint {
  margin: 8px 0 0;
  text-align: center;
  color: #606266;
}

.placeholder-hint {
  color: #909399;
  font-size: 13px;
}
</style>
