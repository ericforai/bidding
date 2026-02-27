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

      <el-table :data="filteredProjects" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="项目名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="goToDetail(row.id)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="customer" label="客户" width="150" />
        <el-table-column prop="budget" label="预算(万元)" width="120" align="right">
          <template #default="{ row }">
            {{ row.budget }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :status="getProgressStatus(row.progress)" />
          </template>
        </el-table-column>
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column prop="deadline" label="截止日期" width="120" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="goToDetail(row.id)">
              查看详情
            </el-button>
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row.id)">
              编辑
            </el-button>
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

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { Search, Refresh, Plus, View, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

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

const filteredProjects = computed(() => {
  let result = [...projectStore.projects]

  if (searchForm.value.name) {
    result = result.filter(p => p.name.includes(searchForm.value.name))
  }
  if (searchForm.value.customer) {
    result = result.filter(p => p.customer.includes(searchForm.value.customer))
  }
  if (searchForm.value.status) {
    result = result.filter(p => p.status === searchForm.value.status)
  }
  if (searchForm.value.manager) {
    result = result.filter(p => p.manager === searchForm.value.manager)
  }

  pagination.value.total = result.length

  const start = (pagination.value.page - 1) * pagination.value.pageSize
  const end = start + pagination.value.pageSize
  return result.slice(start, end)
})

const getStatusType = (status) => {
  const typeMap = {
    drafting: 'info',
    reviewing: 'warning',
    bidding: 'primary',
    won: 'success',
    lost: 'danger',
    pending: 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    drafting: '草稿中',
    reviewing: '评审中',
    bidding: '投标中',
    won: '已中标',
    lost: '未中标',
    pending: '待立项'
  }
  return textMap[status] || status
}

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

const goToDetail = (id) => {
  router.push(`/project/${id}`)
}

const handleEdit = (id) => {
  ElMessage.info('编辑功能开发中')
  // TODO: 实现编辑功能
}

onMounted(() => {
  projectStore.getProjects()
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

.table-card {
  min-height: calc(100vh - 280px);
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
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

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

  /* 表格移动端优化 */
  .table-card :deep(.el-table) {
    font-size: 12px;
  }

  .table-card :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }

  .table-card :deep(.el-table__cell) {
    padding: 8px 4px;
  }

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
</style>
