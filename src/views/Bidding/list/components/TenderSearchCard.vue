<template>
  <el-card class="search-card tender-search-card" shadow="never">
    <el-form :model="modelValue" class="tender-search-form" label-position="top">
      <el-form-item label="关键词" class="search-field search-field--keyword">
        <el-input v-model="modelValue.keyword" placeholder="搜索标题、客户名称" clearable class="search-input">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </el-form-item>
      <el-form-item label="总部所在地" class="search-field">
        <el-select v-model="modelValue.region" placeholder="全部总部所在地" clearable class="filter-select">
          <el-option v-for="item in regions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" class="search-field">
        <el-select v-model="modelValue.status" placeholder="全部状态" clearable class="filter-select">
          <el-option label="待处理" value="PENDING" /><el-option label="跟踪中" value="TRACKING" />
          <el-option label="已投标" value="BIDDED" /><el-option label="已放弃" value="ABANDONED" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源" class="search-field">
        <el-select v-model="modelValue.sourceType" placeholder="全部来源" clearable class="filter-select">
          <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户类型" class="search-field">
        <el-select v-model="modelValue.customerType" placeholder="全部客户类型" clearable class="filter-select">
          <el-option v-for="item in customerTypeOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户级别" class="search-field">
        <el-select v-model="modelValue.priority" placeholder="全部级别" clearable class="filter-select">
          <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="报名截止时间" class="search-field">
        <el-date-picker v-model="registrationDeadlineRange" type="daterange" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" class="filter-date-picker" clearable />
      </el-form-item>
      <el-form-item label="开标时间" class="search-field">
        <el-date-picker v-model="bidOpeningTimeRange" type="datetimerange" range-separator="至"
          start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DD HH:mm:ss" class="filter-date-picker" clearable />
      </el-form-item>
      <el-form-item class="search-actions">
        <el-button type="primary" class="search-submit-button" @click="$emit('search')">
          <el-icon><Search /></el-icon>搜索
        </el-button>
        <el-button class="search-reset-button" @click="$emit('reset')">
          <el-icon><RefreshLeft /></el-icon>重置
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { RefreshLeft, Search } from '@element-plus/icons-vue'
import { REGION_OPTIONS, SOURCE_OPTIONS, CUSTOMER_TYPE_OPTIONS, PRIORITY_OPTIONS } from '../constants.js'
import { computed } from 'vue'

const modelValue = defineModel({ type: Object, required: true })
defineProps({
  regions: { type: Array, default: () => REGION_OPTIONS },
  sourceOptions: { type: Array, default: () => SOURCE_OPTIONS },
  customerTypeOptions: { type: Array, default: () => CUSTOMER_TYPE_OPTIONS },
  priorityOptions: { type: Array, default: () => PRIORITY_OPTIONS },
})
defineEmits(['search', 'reset'])

const registrationDeadlineRange = computed({
  get: () => {
    const { registrationDeadlineFrom: f, registrationDeadlineTo: t } = modelValue.value
    return (f && t) ? [f, t] : null
  },
  set: (val) => {
    modelValue.value.registrationDeadlineFrom = val?.[0] ? val[0] + ' 00:00:00' : null
    modelValue.value.registrationDeadlineTo = val?.[1] ? val[1] + ' 23:59:59' : null
  }
})

const bidOpeningTimeRange = computed({
  get: () => {
    const { bidOpeningTimeFrom: f, bidOpeningTimeTo: t } = modelValue.value
    return (f && t) ? [f, t] : null
  },
  set: (val) => {
    modelValue.value.bidOpeningTimeFrom = val?.[0] || null
    modelValue.value.bidOpeningTimeTo = val?.[1] || null
  }
})
</script>

<style scoped>
.tender-search-card { border: 1px solid var(--gray-100, #E8E8E8); border-radius: var(--radius-md, 8px); }
.tender-search-card :deep(.el-card__body) { padding: var(--space-lg, 24px); }
.tender-search-form { display: flex; flex-wrap: wrap; align-items: flex-end; gap: var(--space-md, 16px) var(--space-lg, 24px); }
.tender-search-form :deep(.el-form-item) { margin: 0; }
.tender-search-form :deep(.el-form-item__label) { margin-bottom: var(--space-xs, 4px); color: var(--text-secondary, #666); font-size: var(--font-size-xs, 12px); font-weight: 600; line-height: 1.4; }
.search-field { flex: 0 0 168px; width: 168px; min-width: 168px; max-width: 168px; }
.search-input, .filter-select { width: 100%; --focus-ring-color: transparent; --focus-ring-width: 0; --el-input-focus-border-color: var(--gray-200, #D0D0D0); --el-input-hover-border-color: var(--gray-200, #D0D0D0); }
.filter-select { --el-color-primary: var(--gray-200, #D0D0D0); --el-color-primary-light-3: var(--gray-200, #D0D0D0); --el-color-primary-light-5: var(--gray-200, #D0D0D0); --el-color-primary-light-7: var(--gray-100, #E8E8E8); --el-select-input-focus-border-color: var(--gray-200, #D0D0D0); }
.tender-search-card :deep(.el-input__wrapper), .tender-search-card :deep(.el-select__wrapper) { height: 40px; min-height: 40px; border: 1px solid var(--gray-100, #E8E8E8); border-radius: var(--radius-sm, 4px); box-sizing: border-box; box-shadow: none; }
.tender-search-card :deep(.el-input), .tender-search-card :deep(.el-select), .tender-search-card :deep(.filter-select *), .tender-search-card :deep(.el-input__wrapper), .tender-search-card :deep(.el-select__wrapper), .tender-search-card :deep(.el-select__input), .tender-search-card :deep(.el-button) { transition: none !important; }
.tender-search-card :deep(.filter-select), .tender-search-card :deep(.filter-select *), .tender-search-card :deep(.search-input), .tender-search-card :deep(.search-input *) { outline: none !important; }
.tender-search-card :deep(.el-input:focus-within), .tender-search-card :deep(.el-select:focus-within), .tender-search-card :deep(.el-button:focus), .tender-search-card :deep(.el-button:focus-visible), .tender-search-card :deep(.el-button:active) { outline: none; box-shadow: none !important; }
.tender-search-card :deep(.el-input__wrapper:focus), .tender-search-card :deep(.el-input__wrapper:focus-visible), .tender-search-card :deep(.el-input__wrapper:active), .tender-search-card :deep(.el-select__wrapper:focus), .tender-search-card :deep(.el-select__wrapper:focus-visible), .tender-search-card :deep(.el-select__wrapper:active) { outline: none !important; box-shadow: none !important; }
.tender-search-card :deep(.el-input__inner), .tender-search-card :deep(.el-input__inner:focus), .tender-search-card :deep(.el-input__inner:focus-visible), .tender-search-card :deep(.el-select .el-input__inner:focus-visible), .tender-search-card :deep(.el-select__input), .tender-search-card :deep(.el-select__input:focus), .tender-search-card :deep(.el-select__input:focus-visible) { outline: none; box-shadow: none !important; }
.tender-search-card :deep(.el-input__wrapper:hover), .tender-search-card :deep(.el-select__wrapper:hover), .tender-search-card :deep(.el-select__wrapper.is-hovering) { border-color: var(--gray-200, #D0D0D0); }
.tender-search-card :deep(.el-input__wrapper.is-focus), .tender-search-card :deep(.el-select__wrapper.is-focused), .tender-search-card :deep(.el-select .el-input.is-focus .el-input__wrapper) { border-color: var(--gray-200, #D0D0D0) !important; box-shadow: none !important; }
.search-actions { flex: 0 0 auto; }
.search-actions :deep(.el-form-item__content) { display: flex; gap: var(--space-sm, 8px); }
.search-submit-button, .search-reset-button { min-width: 88px; height: 40px; border-radius: var(--radius-sm, 4px); box-shadow: none; font-weight: 600; }
.search-submit-button { background: var(--brand-primary, #0066CC); border-color: var(--brand-primary, #0066CC); }
.search-submit-button:focus, .search-submit-button:focus-visible, .search-submit-button:active { border-color: var(--brand-primary, #0066CC); box-shadow: none; outline: none; }
.search-reset-button { border-color: var(--gray-200, #D0D0D0); color: var(--text-secondary, #666); }
.filter-date-picker { width: 240px; --el-date-editor-width: 240px; }
.filter-date-picker :deep(.el-input__wrapper), .filter-date-picker :deep(.el-range-input) { height: 40px; border: 1px solid var(--gray-100, #E8E8E8); border-radius: var(--radius-sm, 4px); box-shadow: none; }
@media (max-width: 768px) {
  .tender-search-card :deep(.el-card__body) { padding: var(--space-md, 16px); }
  .search-field, .search-field--keyword, .search-actions { flex: 1 1 100%; min-width: 0; }
  .filter-date-picker { flex: 1 1 100%; width: 100%; --el-date-editor-width: 100%; }
  .search-actions :deep(.el-form-item__content) { width: 100%; }
  .search-submit-button, .search-reset-button { flex: 1; }
}
</style>
