<template>
  <el-card class="b2b-feature-card feature-card" :class="{ disabled: !feature.enabled }" shadow="never">
    <!-- 卡片头部 -->
    <div class="card-header">
      <div class="header-left">
        <span class="feature-icon">{{ feature.icon }}</span>
        <span class="feature-name">{{ feature.name }}</span>
      </div>
      <div class="header-right">
        <el-switch
          :model-value="feature.enabled"
          @change="handleToggle"
          inline-prompt
          active-text="启用"
          inactive-text="禁用"
        />
        <el-button
          type="primary"
          link
          @click="handleConfigure"
        >
          <el-icon><Setting /></el-icon>
          配置
        </el-button>
      </div>
    </div>

    <el-divider />

    <!-- 卡片内容 -->
    <div class="card-content">
      <!-- 说明 -->
      <div class="content-row">
        <span class="label">说明：</span>
        <span class="value">{{ feature.description }}</span>
      </div>

      <!-- 统计 -->
      <div class="content-row">
        <span class="label">统计：</span>
        <div class="stats-tags">
          <el-tag type="info" size="small">
            本月使用 {{ feature.stats.usageCount }} 次
          </el-tag>
          <el-tag :type="getAccuracyType(feature.stats.accuracy)" size="small">
            准确率 {{ feature.stats.accuracy }}%
          </el-tag>
        </div>
      </div>

      <!-- 提示词预览 -->
      <div class="content-row prompt-row">
        <span class="label">提示词：</span>
        <div class="prompt-preview">
          <el-icon class="prompt-icon"><ChatDotRound /></el-icon>
          <span class="prompt-text">{{ truncatedPrompt }}</span>
          <el-tooltip :content="feature.promptTemplate" placement="top">
            <el-button type="info" link size="small">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { Setting, ChatDotRound, MoreFilled } from '@element-plus/icons-vue'

const props = defineProps({
  feature: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['toggle', 'configure'])

// 截断提示词预览（前50字）
const truncatedPrompt = computed(() => {
  const prompt = props.feature.promptTemplate || ''
  return prompt.length > 50 ? prompt.substring(0, 50) + '...' : prompt
})

// 根据准确率返回标签类型
const getAccuracyType = (accuracy) => {
  if (accuracy >= 95) return 'success'
  if (accuracy >= 90) return ''
  if (accuracy >= 85) return 'warning'
  return 'danger'
}

const handleToggle = (val) => {
  emit('toggle', props.feature.id, val)
}

const handleConfigure = () => {
  console.log('[FeatureCard] handleConfigure called, featureId:', props.feature.id)
  emit('configure', props.feature.id)
  console.log('[FeatureCard] emit done')
}
</script>

<script>
export default {
  name: 'FeatureCard'
}
</script>

<style scoped>
/* Using B2B feature card styles - minimal custom overrides */
.feature-card {
  transition: all 0.25s ease;
}

.feature-card.disabled {
  opacity: 0.6;
}

.feature-card :deep(.el-card__body) {
  padding: var(--card-padding, 20px);
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feature-icon {
  font-size: 24px;
}

.feature-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 分隔线 */
.el-divider {
  margin: 16px 0;
}

/* 卡片内容 */
.card-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.content-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.label {
  flex-shrink: 0;
  font-size: 13px;
  color: #666666;
  font-weight: 500;
}

.value {
  font-size: 13px;
  color: #1a1a1a;
  line-height: 1.6;
}

.stats-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 提示词预览 */
.prompt-row {
  align-items: center;
}

.prompt-preview {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
  font-size: 12px;
}

.prompt-icon {
  color: var(--color-primary, #0066CC);
  flex-shrink: 0;
}

.prompt-text {
  flex: 1;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 480px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-right {
    width: 100%;
    justify-content: space-between;
  }

  .stats-tags {
    flex-direction: column;
  }
}
</style>
