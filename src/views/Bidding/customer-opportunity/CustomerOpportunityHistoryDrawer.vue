<template>
  <el-drawer v-model="drawerVisible" title="历史采购全景图" size="600px" class="premium-drawer">
    <div v-if="selectedCustomer && customerHistory.length" class="panoramic-view">
      <div class="panoramic-stats">
        <div class="stat-card blue">
          <span class="stat-label">累计采购项目</span>
          <p class="stat-value">{{ drawerStats.totalCount }}</p>
        </div>
        <div class="stat-card purple">
          <span class="stat-label">累计预算总额</span>
          <p class="stat-value">¥{{ drawerStats.totalBudget }}<small>万</small></p>
        </div>
        <div class="stat-card green">
          <span class="stat-label">首选品类</span>
          <p class="stat-value">{{ drawerStats.topCategory }}</p>
        </div>
      </div>

      <div class="panoramic-section">
        <h4 class="section-title">品类购买频率分布</h4>
        <div class="category-bars">
          <div v-for="cat in categoryStats" :key="cat.name" class="cat-bar-item">
            <div class="cat-info">
              <span>{{ cat.name }}</span>
              <span>{{ cat.count }}次 ({{ cat.percent }}%)</span>
            </div>
            <div class="cat-progress-bg">
              <div class="cat-progress-fill" :style="{ width: `${cat.percent}%`, backgroundColor: cat.color }"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="panoramic-section">
        <h4 class="section-title">采购历史全轨迹</h4>
        <div class="history-scroll-list">
          <div class="history-item-card" v-for="record in customerHistory" :key="record.recordId">
            <div class="h-item-top">
              <el-tag size="small" :type="record.budget > 500 ? 'danger' : 'info'" effect="light">¥{{ record.budget }}万</el-tag>
              <span class="h-item-date">{{ record.publishDate }}</span>
            </div>
            <p class="h-item-title">{{ record.title }}</p>
            <div class="h-item-footer">
              <span class="h-item-cat"><el-icon><MagicStick /></el-icon> {{ record.category }}</span>
              <div class="h-item-tags">
                <span v-for="tag in record.extractedTags" :key="tag" class="micro-tag">{{ tag }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="panoramic-empty">
      <el-empty description="该客户暂无更多历史记录" />
    </div>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  selectedCustomer: {
    type: Object,
    default: null
  },
  customerHistory: {
    type: Array,
    default: () => []
  },
  drawerStats: {
    type: Object,
    default: () => ({})
  },
  categoryStats: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})
</script>
