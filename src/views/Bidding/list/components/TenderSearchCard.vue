<template>
  <el-card class="search-card" shadow="never">
    <el-form :model="modelValue" inline>
      <el-form-item label="关键词">
        <el-input v-model="modelValue.keyword" placeholder="搜索标题、客户名称" clearable class="search-input">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="地区">
        <el-select v-model="modelValue.region" placeholder="全部地区" clearable class="filter-select">
          <el-option v-for="item in regions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="行业">
        <el-select v-model="modelValue.industry" placeholder="全部行业" clearable class="filter-select">
          <el-option v-for="item in industries" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="modelValue.status" placeholder="全部状态" clearable class="filter-select">
          <el-option label="待处理" value="PENDING" />
          <el-option label="跟踪中" value="TRACKING" />
          <el-option label="已投标" value="BIDDED" />
          <el-option label="已放弃" value="ABANDONED" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源">
        <el-select v-model="modelValue.source" placeholder="全部来源" clearable class="filter-select">
          <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="$emit('search')">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="$emit('reset')">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import { INDUSTRY_OPTIONS, REGION_OPTIONS, SOURCE_OPTIONS } from '../constants.js'

defineProps({
  modelValue: { type: Object, required: true },
  regions: { type: Array, default: () => REGION_OPTIONS },
  industries: { type: Array, default: () => INDUSTRY_OPTIONS },
  sourceOptions: { type: Array, default: () => SOURCE_OPTIONS },
})

defineEmits(['search', 'reset'])
</script>
