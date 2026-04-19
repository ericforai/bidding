<template>
  <el-card class="table-card">
    <el-table v-loading="loading" :data="templates" stripe style="width: 100%">
      <el-table-column prop="name" label="模板名称" min-width="220">
        <template #default="{ row }">
          <div class="name-cell">
            <el-icon class="category-icon" :color="getCategoryColor(row.category)">
              <component :is="resolveCategoryIcon(row.category)" />
            </el-icon>
            <div class="name-content">
              <span class="name-text">{{ row.name }}</span>
              <span class="name-desc">{{ row.description }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="category" label="历史大类" width="120">
        <template #default="{ row }">
          <el-tag :type="getCategoryTagType(row.category)" size="small">
            {{ getCategoryLabel(row.category) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="三维分类" min-width="260">
        <template #default="{ row }">
          <div class="classification-cell">
            <el-tag v-if="row.productType" size="small" effect="plain">{{ row.productType }}</el-tag>
            <el-tag v-if="row.industry" size="small" effect="plain" type="success">{{ row.industry }}</el-tag>
            <el-tag v-if="row.documentType" size="small" effect="plain" type="warning">{{ row.documentType }}</el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="tags" label="标签" min-width="180">
        <template #default="{ row }">
          <div class="classification-cell">
            <el-tag v-for="tag in row.tags.slice(0, 3)" :key="tag" size="small" effect="plain">
              {{ tag }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="downloads" label="下载量" width="120">
        <template #default="{ row }">{{ formatNumber(row.downloads) }}</template>
      </el-table-column>

      <el-table-column prop="updateTime" label="更新时间" width="120">
        <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
      </el-table-column>

      <el-table-column prop="version" label="版本" width="90">
        <template #default="{ row }">
          <el-tag type="info" size="small">v{{ row.version }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="View" size="small" @click="$emit('preview', row)">预览</el-button>
          <el-button type="success" link :icon="DocumentAdd" size="small" @click="$emit('use-template', row)">
            一键使用
          </el-button>
          <el-dropdown @command="(command) => $emit('more-action', command, row)">
            <el-button type="info" link :icon="MoreFilled" size="small">更多</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit" :icon="Edit">编辑模板</el-dropdown-item>
                <el-dropdown-item command="copy" :icon="CopyDocument">复制模板</el-dropdown-item>
                <el-dropdown-item command="version" :icon="Clock">版本历史</el-dropdown-item>
                <el-dropdown-item command="download" :icon="Download">下载</el-dropdown-item>
                <el-dropdown-item command="delete" :icon="Delete" divided>删除模板</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @update:current-page="$emit('update:page', $event)"
        @update:page-size="$emit('update:page-size', $event)"
      />
    </div>
  </el-card>
</template>

<script setup>
import {
  Clock,
  CopyDocument,
  Delete,
  Document,
  DocumentAdd,
  DocumentCopy,
  Download,
  Edit,
  Medal,
  MoreFilled,
  Notebook,
  Operation,
  Tickets,
  View
} from '@element-plus/icons-vue'
import { formatDate, formatNumber } from './templateLibraryHelpers.js'
import {
  getCategoryColor,
  getCategoryLabel,
  getCategoryTagType,
  normalizeTemplateCategory
} from '@/config/templateLibrary.js'

const iconMap = {
  technical: Document,
  commercial: DocumentCopy,
  implementation: Operation,
  quotation: Tickets,
  qualification: Medal,
  contract: Notebook
}

defineProps({
  templates: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  page: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  total: { type: Number, required: true }
})

defineEmits(['preview', 'use-template', 'more-action', 'update:page', 'update:page-size'])

function resolveCategoryIcon(category) {
  return iconMap[normalizeTemplateCategory(category)] || Document
}
</script>

<style scoped>
.table-card {
  margin-top: 20px;
}

.name-cell {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.category-icon {
  margin-top: 2px;
}

.name-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.name-text {
  font-weight: 600;
}

.name-desc {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.classification-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
