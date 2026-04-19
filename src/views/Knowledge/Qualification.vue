<template>
  <div class="qualification-container">
    <QualificationSearchCard
      :search-form="searchForm"
      @export="handleExportList"
      @reset="handleReset"
      @search="handleSearch"
      @upload="handleUpload"
    />

    <QualificationTableCard
      :feature-placeholder="listFeaturePlaceholder"
      :is-admin="isAdmin"
      :loading="listLoading"
      :page="pagination.page"
      :page-size="pagination.pageSize"
      :qualifications="pagedQualifications"
      :total="filteredQualifications.length"
      @borrow="openBorrowDialog"
      @delete="handleDelete"
      @download="handleDownload"
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
      @view="handleView"
    />

    <QualificationBorrowHistoryCard
      :feature-placeholder="borrowFeaturePlaceholder"
      :loading="borrowLoading"
      :records="borrowRecords"
      @create="openBorrowDialog(null)"
      @return="handleReturn"
    />

    <QualificationUploadDialog
      v-model="uploadDialogVisible"
      :form="uploadForm"
      @confirm="handleConfirmUpload"
      @file-change="handleFileChange"
    />

    <QualificationDetailDialog
      v-model="detailDialogVisible"
      :qualification="currentQualification"
      @download="handleDownload"
    />

    <QualificationBorrowDialog
      v-model="borrowDialogVisible"
      :feature-placeholder="borrowFeaturePlaceholder"
      :form="borrowForm"
      :qualification="currentQualification"
      @confirm="handleConfirmBorrow"
    />
  </div>
</template>

<script setup>
import QualificationBorrowDialog from './components/qualification/QualificationBorrowDialog.vue'
import QualificationBorrowHistoryCard from './components/qualification/QualificationBorrowHistoryCard.vue'
import QualificationDetailDialog from './components/qualification/QualificationDetailDialog.vue'
import QualificationSearchCard from './components/qualification/QualificationSearchCard.vue'
import QualificationTableCard from './components/qualification/QualificationTableCard.vue'
import QualificationUploadDialog from './components/qualification/QualificationUploadDialog.vue'
import { useQualificationPage } from './components/qualification/useQualificationPage.js'

const {
  borrowDialogVisible,
  borrowFeaturePlaceholder,
  borrowForm,
  borrowLoading,
  borrowRecords,
  currentQualification,
  detailDialogVisible,
  filteredQualifications,
  handleConfirmBorrow,
  handleConfirmUpload,
  handleDelete,
  handleDownload,
  handleExportList,
  handleFileChange,
  handlePageChange,
  handleReset,
  handleReturn,
  handleSearch,
  handleSizeChange,
  handleUpload,
  handleView,
  isAdmin,
  listFeaturePlaceholder,
  listLoading,
  openBorrowDialog,
  pagedQualifications,
  pagination,
  searchForm,
  uploadDialogVisible,
  uploadForm
} = useQualificationPage()
</script>

<style scoped lang="scss">
.qualification-container {
  padding: 20px;
}
</style>
