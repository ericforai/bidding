<template>
  <div class="template-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">模板库</h2>
        <p class="page-subtitle">历史大类页签保留为视图入口，产品类型、行业、文档类型是正式分类维度。</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">
          新建模板
        </el-button>
      </div>
    </div>

    <div class="category-tabs">
      <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange">
        <el-tab-pane
          v-for="tab in categoryTabs"
          :key="tab.name"
          :name="tab.name"
        >
          <template #label>
            <span class="tab-label">
              <el-icon><component :is="tab.icon" /></el-icon>
              {{ tab.label }}
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <TemplateFilterPanel
      :filters="filters"
      :all-tags="allTags"
      :product-type-options="PRODUCT_TYPE_OPTIONS"
      :industry-options="INDUSTRY_OPTIONS"
      :document-type-options="DOCUMENT_TYPE_OPTIONS"
      @search="handleSearch"
      @reset="handleReset"
    />

    <FeaturePlaceholder
      v-if="featurePlaceholder"
      class="placeholder-card"
      :title="featurePlaceholder.title"
      :message="featurePlaceholder.message"
      :hint="featurePlaceholder.hint"
    />
    <TemplateListTable
      v-else
      :templates="pagedTemplates"
      :loading="loading"
      :page="pagination.page"
      :page-size="pagination.pageSize"
      :total="filteredTemplates.length"
      @preview="handlePreview"
      @use-template="handleUseTemplate"
      @more-action="handleMoreAction"
      @update:page="pagination.page = $event"
      @update:page-size="pagination.pageSize = $event"
    />

    <TemplatePreviewDialog
      v-model:visible="previewDialogVisible"
      v-model:active-tab="activePreviewTab"
      :template="previewTemplate"
      @use-template="handleUseTemplate"
      @download="handleDownload"
    />

    <TemplateUseDialog
      v-model:visible="useTemplateDialogVisible"
      :template="selectedTemplate"
      :form="useTemplateForm"
      :projects="inProgressProjects"
      @confirm="confirmUseTemplate"
    />

    <TemplateUpsertDialog
      v-model:visible="upsertDialogVisible"
      :mode="upsertMode"
      :form="templateForm"
      :category-options="categoryOptions"
      :product-type-options="PRODUCT_TYPE_OPTIONS"
      :industry-options="INDUSTRY_OPTIONS"
      :document-type-options="DOCUMENT_TYPE_OPTIONS"
      :submitting="upsertSubmitting"
      @submit="submitTemplate"
    />

    <TemplateVersionDialog
      v-model:visible="versionDialogVisible"
      :versions="versionHistory"
      :placeholder="versionPlaceholder"
    />
  </div>
</template>

<script setup>
import {
  Document,
  DocumentCopy,
  Grid,
  Medal,
  Notebook,
  Operation,
  Plus,
  Tickets
} from '@element-plus/icons-vue'
import FeaturePlaceholder from '@/components/common/FeaturePlaceholder.vue'
import {
  DOCUMENT_TYPE_OPTIONS,
  INDUSTRY_OPTIONS,
  PRODUCT_TYPE_OPTIONS,
  TEMPLATE_CATEGORY_OPTIONS
} from '@/config/templateLibrary.js'
import TemplateFilterPanel from './components/template/TemplateFilterPanel.vue'
import TemplateListTable from './components/template/TemplateListTable.vue'
import TemplatePreviewDialog from './components/template/TemplatePreviewDialog.vue'
import TemplateUpsertDialog from './components/template/TemplateUpsertDialog.vue'
import TemplateUseDialog from './components/template/TemplateUseDialog.vue'
import TemplateVersionDialog from './components/template/TemplateVersionDialog.vue'
import { useTemplateLibraryPage } from './components/template/useTemplateLibraryPage.js'

const categoryTabs = [
  { name: 'all', label: '全部', icon: Grid },
  { name: 'technical', label: '技术方案', icon: Document },
  { name: 'commercial', label: '商务文件', icon: DocumentCopy },
  { name: 'implementation', label: '实施方案', icon: Operation },
  { name: 'quotation', label: '报价清单', icon: Tickets },
  { name: 'qualification', label: '资质文件', icon: Medal },
  { name: 'contract', label: '合同范本', icon: Notebook }
]

const categoryOptions = TEMPLATE_CATEGORY_OPTIONS

const {
  activeCategory,
  filters,
  pagination,
  loading,
  featurePlaceholder,
  versionPlaceholder,
  previewDialogVisible,
  previewTemplate,
  activePreviewTab,
  useTemplateDialogVisible,
  selectedTemplate,
  useTemplateForm,
  versionDialogVisible,
  versionHistory,
  upsertDialogVisible,
  upsertMode,
  templateForm,
  upsertSubmitting,
  inProgressProjects,
  allTags,
  filteredTemplates,
  pagedTemplates,
  handleSearch,
  handleReset,
  handleCategoryChange,
  openCreateDialog,
  submitTemplate,
  handlePreview,
  handleUseTemplate,
  confirmUseTemplate,
  handleMoreAction,
  handleDownload
} = useTemplateLibraryPage()
</script>

<style scoped lang="scss">
.template-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 6px;
}

.page-subtitle {
  margin: 0;
  color: #6b7280;
  line-height: 1.5;
}

.category-tabs {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #dbeafe;
  border-radius: 16px;
  padding: 0 20px;
  margin-bottom: 20px;

  :deep(.el-tabs__header) {
    margin: 0;
  }
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.placeholder-card {
  margin-top: 20px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
