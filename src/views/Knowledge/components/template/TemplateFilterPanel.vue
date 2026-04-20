<template>
  <el-card class="search-card">
    <el-form :inline="true" :model="filters" class="template-filter-form">
      <el-form-item label="模板名称">
        <el-input
          v-model="filters.name"
          aria-label="模板名称"
          placeholder="搜索模板名称"
          clearable
          :prefix-icon="Search"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="产品类型">
        <el-select
          v-model="filters.productType"
          aria-label="产品类型"
          placeholder="选择产品类型"
          clearable
          style="width: 180px"
        >
          <el-option
            v-for="option in productTypeOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="行业">
        <el-select
          v-model="filters.industry"
          aria-label="行业"
          placeholder="选择行业"
          clearable
          style="width: 180px"
        >
          <el-option
            v-for="option in industryOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="文档类型">
        <el-select
          v-model="filters.documentType"
          aria-label="文档类型"
          placeholder="选择文档类型"
          clearable
          style="width: 180px"
        >
          <el-option
            v-for="option in documentTypeOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="标签">
        <el-select
          v-model="filters.tags"
          placeholder="选择标签"
          multiple
          collapse-tags
          clearable
          style="width: 220px"
        >
          <el-option
            v-for="tag in allTags"
            :key="tag"
            :label="tag"
            :value="tag"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="排序">
        <el-select v-model="filters.sort" aria-label="排序" placeholder="默认排序" style="width: 160px">
          <el-option label="默认排序" value="default" />
          <el-option label="下载量" value="downloads" />
          <el-option label="更新时间" value="updateTime" />
          <el-option label="名称" value="name" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="$emit('search')">
          搜索
        </el-button>
        <el-button @click="$emit('reset')">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'

defineProps({
  filters: {
    type: Object,
    required: true
  },
  allTags: {
    type: Array,
    default: () => []
  },
  productTypeOptions: {
    type: Array,
    default: () => []
  },
  industryOptions: {
    type: Array,
    default: () => []
  },
  documentTypeOptions: {
    type: Array,
    default: () => []
  }
})

defineEmits(['search', 'reset'])
</script>

<style scoped>
.template-filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 0;
}
</style>
