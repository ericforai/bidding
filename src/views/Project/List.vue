<template>
  <div class="project-list-container">
    <el-card class="search-card b2b-section-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="项目名称">
          <el-input v-model="searchForm.name" placeholder="请输入项目名称" clearable />
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="searchForm.customer" placeholder="请输入客户名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="全部" value="" />
            <el-option label="草稿中" value="drafting" />
            <el-option label="评审中" value="reviewing" />
            <el-option label="投标中" value="bidding" />
            <el-option label="已中标" value="won" />
            <el-option label="未中标" value="lost" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="searchForm.manager" placeholder="请选择负责人" clearable>
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.name"
              :value="user.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card b2b-section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">投标项目列表</span>
          <el-button type="primary" :icon="Plus" @click="goToCreate">创建项目</el-button>
        </div>
      </template>

      <el-table :data="filteredProjects" stripe class="project-table" style="width: 100%" v-loading="loading">
        <el-table-column label="项目信息" min-width="420">
          <template #default="{ row }">
            <div class="project-name-cell">
              <el-link class="project-title-link" type="primary" @click="goToDetail(row.id)">{{ row.name }}</el-link>
              <div class="project-meta"><span class="meta-label">客户</span><span class="meta-value">{{ row.customer }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="budget" label="预算(万元)" width="120" align="right">
          <template #default="{ row }">{{ row.budget }}</template>
        </el-table-column>
        <el-table-column label="状态/进度" width="180">
          <template #default="{ row }">
            <div class="status-progress-cell">
              <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag><el-progress class="project-progress" :percentage="row.progress" :status="getProgressStatus(row.progress)" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="负责人/截止" width="150">
          <template #default="{ row }">
            <div class="owner-deadline-cell">
              <span class="owner-name">{{ row.manager || '未分配' }}</span><span class="deadline-text">{{ row.deadline || '未设置' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="104" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-tooltip content="查看详情" placement="top"><el-button class="table-action-button" text type="primary" :icon="View" aria-label="查看详情" @click="goToDetail(row.id)" /></el-tooltip>
              <el-tooltip content="编辑" placement="top"><el-button class="table-action-button" text type="primary" :icon="Edit" aria-label="编辑" @click="handleEdit(row.id)" /></el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<!--
 Input: useProjectStore, useUserStore (from @/stores), vue-router
 Output: ProjectList component - 投标项目列表页面（搜索、表格布局、分页）
 Pos: src/views/Project/ - 视图层
 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
-->
<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { Search, Refresh, Plus, View, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getProjectStatusText, getProjectStatusType } from './project-utils.js'

const router = useRouter()
const projectStore = useProjectStore()
const userStore = useUserStore()

const loading = ref(false)
const searchForm = ref({
  name: '',
  customer: '',
  status: '',
  manager: ''
})

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const userList = computed(() => userStore.users)

const matchedProjects = computed(() => {
  const { name, customer, status, manager } = searchForm.value
  return projectStore.projects.filter((p) =>
    (!name || p.name.includes(name))
    && (!customer || p.customer.includes(customer))
    && (!status || p.status === status)
    && (!manager || p.manager === manager)
  )
})

const filteredProjects = computed(() => {
  const start = (pagination.value.page - 1) * pagination.value.pageSize
  return matchedProjects.value.slice(start, start + pagination.value.pageSize)
})

watch(() => matchedProjects.value.length, (total) => {
  pagination.value.total = total
}, { immediate: true })

const getStatusType = (status) => getProjectStatusType(status)
const getStatusText = (status) => getProjectStatusText(status)

const getProgressStatus = (progress) => {
  if (progress === 100) return 'success'
  if (progress >= 80) return undefined
  return undefined
}

const handleSearch = () => {
  pagination.value.page = 1
}

const handleReset = () => {
  searchForm.value = {
    name: '',
    customer: '',
    status: '',
    manager: ''
  }
  pagination.value.page = 1
}

const handleSizeChange = () => {
  pagination.value.page = 1
}

const handlePageChange = () => {}

const goToCreate = () => {
  router.push('/project/create')
}

const goToDetail = async (id) => {
  ElMessage.success(`正在跳转到项目详情: ${id}`)
  try {
    await router.push(`/project/${id}`)
  } catch (error) {
    ElMessage.error(`跳转失败: ${error.message}`)
  }
}

const handleEdit = async (id) => {
  try {
    // 跳转到创建页面，带上编辑 ID 参数
    await router.push({
      name: 'ProjectCreate',
      query: { editId: id }
    })
  } catch (error) {
    ElMessage.error(`跳转失败: ${error.message}`)
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await projectStore.getProjects()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.project-list-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.project-table { border: 1px solid #eef2f0; border-radius: 10px; overflow: hidden; }
.project-table :deep(.el-table__header-wrapper th.el-table__cell) { height: 48px; background: #f7faf8; color: #334155; font-weight: 600; }
.project-table :deep(.el-table__cell) { padding: 14px 0; border-bottom-color: #edf2f0; vertical-align: middle; }
.project-table :deep(.el-table__row:hover > td.el-table__cell) { background: #f8fbfa; }
.project-name-cell { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.project-title-link { align-self: flex-start; justify-content: flex-start; } .project-title-link :deep(.el-link__inner) { white-space: normal; line-height: 1.45; text-align: left; font-weight: 600; color: #14796b; }
.project-meta { display: inline-flex; align-items: center; gap: 6px; color: #64748b; font-size: 13px; line-height: 1.4; } .meta-label { padding: 1px 6px; border-radius: 4px; background: #eef2f6; color: #475569; font-size: 12px; } .deadline-text { color: #64748b; font-size: 13px; line-height: 1.4; }
.status-progress-cell, .owner-deadline-cell { display: flex; flex-direction: column; gap: 8px; }
.project-progress :deep(.el-progress__text) { min-width: 28px; color: #475569; }
.owner-name { color: #334155; font-weight: 500; line-height: 1.4; }
.table-actions { display: inline-flex; align-items: center; justify-content: center; gap: 6px; }
.table-actions :deep(.table-action-button) { width: 30px; height: 30px; padding: 0; margin-left: 0; border-radius: 8px; background: #eef7f3; }
.table-actions :deep(.table-action-button:hover) { background: #dcefe7; color: var(--brand-xiyu-logo-active, #1F553F); }

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .project-list-container {
    padding: 12px;
  }

  .search-card {
    margin-bottom: 12px;
  }

  .search-form :deep(.el-form) {
    display: block;
  }

  .search-form :deep(.el-form-item) {
    display: block;
    margin-right: 0;
    margin-bottom: 12px;
  }

  .search-form :deep(.el-input),
  .search-form :deep(.el-select) {
    width: 100% !important;
  }

  .search-form :deep(.el-button) {
    width: 100%;
    margin-bottom: 8px;
  }

  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .card-header .el-button {
    width: 100%;
  }

  .table-card :deep(.el-table) { font-size: 12px; }
  .table-card :deep(.el-table__cell) { padding: 8px 4px; }

  /* 分页移动端优化 */
  .pagination {
    justify-content: center;
  }

  .pagination :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }

  .pagination :deep(.el-pagination__sizes),
  .pagination :deep(.el-pagination__jump) {
    display: none;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .search-form :deep(.el-button),
  .card-header .el-button {
    min-height: 44px;
  }
}

/* ==================== Button Enhancements ==================== */

.card-header .el-button {
  min-width: 110px;
  height: 38px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.card-header .el-button--primary {
  background: linear-gradient(135deg, var(--brand-xiyu-logo, #2E7659), var(--brand-xiyu-logo-hover, #27674E));
  border: none;
  box-shadow: 0 2px 8px var(--brand-xiyu-logo-shadow, rgba(46, 118, 89, 0.24));
}

.card-header .el-button--primary:hover {
  background: linear-gradient(135deg, #367F61, var(--brand-xiyu-logo-active, #1F553F));
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(46, 118, 89, 0.3);
}

.card-header .el-button--primary:active {
  transform: translateY(0);
}

/* Search form buttons */
.search-form .el-button {
  height: 36px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  padding: 0 20px;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.search-form .el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(46, 118, 89, 0.25);
}

.search-form .el-button--default:hover {
  border-color: #94a3b8;
  color: #1e293b;
  background: #f8fafc;
}

/* ==================== Input Field Enhancements ==================== */

.search-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: none;
  box-shadow: none;
}

.search-form :deep(.el-input__wrapper:hover) {
  border-color: #e5e7eb;
  box-shadow: none;
}

/* Select dropdown */
.search-form :deep(.el-select__wrapper) {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: none;
}

.search-form :deep(.el-select__wrapper:hover) {
  border-color: #e5e7eb;
  box-shadow: none;
}

/* Form labels */
.search-form :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

/* ==================== Table Link Enhancements ==================== */

:deep(.el-link) {
  font-size: 14px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-link:hover) {
  transform: translateX(2px);
}

/* ==================== Tag Enhancements ==================== */

:deep(.el-tag) {
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border: none;
}

:deep(.el-tag--primary) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #ffffff;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #ffffff;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #ffffff;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #ffffff;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #64748b, #475569);
  color: #ffffff;
}

/* ==================== Progress Bar Enhancement ==================== */

:deep(.el-progress__bar) {
  border-radius: 10px;
}

:deep(.el-progress-bar__outer) {
  border-radius: 10px;
  background: #f1f5f9;
}

/* ==================== Pagination Enhancement ==================== */

.pagination :deep(.el-pagination) {
  gap: 8px;
}

.pagination :deep(.el-pager li) {
  border-radius: 8px;
  min-width: 36px;
  height: 36px;
  font-size: 14px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.pagination :deep(.el-pager li:hover) {
  background: #f1f5f9;
}

.pagination :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, var(--brand-xiyu-logo, #2E7659), var(--brand-xiyu-logo-hover, #27674E));
  color: #ffffff;
}

.pagination :deep(.btn-prev),
.pagination :deep(.btn-next) {
  border-radius: 8px;
  width: 36px;
  height: 36px;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.pagination :deep(.btn-prev:hover),
.pagination :deep(.btn-next:hover) {
  background: #f1f5f9;
  color: var(--brand-xiyu-logo, #2E7659);
}
</style>
