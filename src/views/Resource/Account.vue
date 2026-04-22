<template>
  <div class="account-page">
    <el-card class="search-card">
      <el-form :inline="true">
        <el-form-item label="平台名称">
          <el-input v-model="searchForm.platform" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="可用" value="available" />
            <el-option label="使用中" value="in_use" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAccounts">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>平台账户管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 添加账户
          </el-button>
        </div>
      </template>

      <el-table :data="accounts" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="platform" label="平台名称" min-width="180">
          <template #default="{ row }">
            <div class="platform-info">
              <el-icon class="platform-icon"><Platform /></el-icon>
              <span>{{ row.platform }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="password" label="密码" width="100">
          <template #default="{ row }">
            <div class="password-cell">
              <span class="password-text">{{ passwordVisible[row.id] ? row.password : '•••' }}</span>
              <el-button
                :icon="passwordVisible[row.id] ? Hide : View"
                link
                type="primary"
                size="small"
                @click="togglePasswordVisibility(row.id)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'available'" type="success">可用</el-tag>
            <el-tag v-else-if="row.status === 'in_use'" type="warning">使用中</el-tag>
            <el-tag v-else type="info">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastUsed" label="最近使用" width="120" />
        <el-table-column prop="borrower" label="使用人" width="100">
          <template #default="{ row }">
            {{ row.borrower || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="借阅" placement="top">
                <el-button
                  :icon="Key"
                  circle
                  size="small"
                  :type="row.status === 'available' ? 'primary' : 'info'"
                  :disabled="row.status !== 'available'"
                  @click="handleBorrow(row)"
                />
              </el-tooltip>
              <el-tooltip content="编辑" placement="top">
                <el-button
                  :icon="Edit"
                  circle
                  size="small"
                  type="warning"
                  @click="handleEdit(row)"
                />
              </el-tooltip>
              <el-tooltip content="复制密码" placement="top">
                <el-button
                  :icon="CopyDocument"
                  circle
                  size="small"
                  type="success"
                  @click="handleCopyPassword(row)"
                />
              </el-tooltip>
              <el-dropdown trigger="click" @command="(cmd) => handleMoreAction(cmd, row)">
                <el-button :icon="MoreFilled" circle size="small" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="view" :icon="View">查看详情</el-dropdown-item>
                    <el-dropdown-item command="reset" :icon="RefreshLeft">重置密码</el-dropdown-item>
                    <el-dropdown-item command="toggle" :icon="View">
                      {{ row.status === 'available' ? '禁用账户' : '启用账户' }}
                    </el-dropdown-item>
                    <el-dropdown-item divided command="delete" :icon="Delete" style="color: #f56c6c">
                      删除账户
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 借阅对话框 -->
    <el-dialog v-model="showBorrowDialog" title="账户借阅申请" width="500px">
      <el-form :model="borrowForm" label-width="100px">
        <el-form-item label="借用平台">
          <el-input v-model="borrowForm.platform" disabled />
        </el-form-item>
        <el-form-item label="关联项目">
          <el-select v-model="borrowForm.project" placeholder="请选择">
            <el-option label="某央企智慧办公平台采购" value="P001" />
            <el-option label="华南电力集团集采项目" value="P002" />
          </el-select>
        </el-form-item>
        <el-form-item label="借用用途">
          <el-radio-group v-model="borrowForm.purpose">
            <el-radio value="购买标书">购买标书</el-radio>
            <el-radio value="投标上传">投标上传</el-radio>
            <el-radio value="其他">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="借用期限">
          <el-date-picker v-model="borrowForm.returnDate" type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="borrowForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBorrowDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitBorrow">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="账户详情" width="560px">
      <el-descriptions v-if="currentAccountDetail" :column="1" border>
        <el-descriptions-item label="平台名称">{{ currentAccountDetail.platform }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentAccountDetail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentAccountDetail.status === 'available'" type="success">可用</el-tag>
          <el-tag v-else-if="currentAccountDetail.status === 'in_use'" type="warning">使用中</el-tag>
          <el-tag v-else type="info">禁用</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="使用人">{{ currentAccountDetail.borrower || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近使用">{{ currentAccountDetail.lastUsed || '-' }}</el-descriptions-item>
        <el-descriptions-item label="归还截止">{{ currentAccountDetail.dueAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="showCreateDialog" :title="editingAccountId ? '编辑账户' : '新增账户'" width="520px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="平台名称">
          <el-input v-model="createForm.accountName" placeholder="请输入平台名称" />
        </el-form-item>
        <el-form-item label="平台类型">
          <el-select v-model="createForm.platformType" placeholder="请选择">
            <el-option label="投标平台" value="BID_PLATFORM" />
            <el-option label="采购平台" value="PROCUREMENT_PLATFORM" />
            <el-option label="政府平台" value="GOVERNMENT_PLATFORM" />
            <el-option label="其他平台" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="createForm.password" type="password" show-password :placeholder="editingAccountId ? '留空则不修改密码' : '请输入密码'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Platform, View, Edit, Delete, CopyDocument, MoreFilled, Key, RefreshLeft, Hide } from '@element-plus/icons-vue'
import { resourcesApi } from '@/api'
import { useUserStore } from '@/stores/user'

const searchForm = ref({
  platform: '',
  status: ''
})

const userStore = useUserStore()

// 密码显示状态
const passwordVisible = ref({})

const accounts = ref([])
const showBorrowDialog = ref(false)
const showDetailDialog = ref(false)
const showCreateDialog = ref(false)
const currentAccount = ref(null)
const currentAccountDetail = ref(null)
const borrowForm = ref({
  platform: '',
  project: '',
  purpose: '购买标书',
  returnDate: '',
  remark: ''
})
const createForm = ref({
  accountName: '',
  platformType: 'BID_PLATFORM',
  username: '',
  password: '' })
const editingAccountId = ref(null)

const loadAccounts = async () => {
  try {
    const response = await resourcesApi.accounts.getList(searchForm.value)
    if (!response?.success) {
      ElMessage.error(response?.message || '账户数据加载失败')
      accounts.value = []
      return
    }
    accounts.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    console.error('Failed to load accounts:', error)
    accounts.value = []
    ElMessage.error('账户数据加载失败')
  }
}

const handleBorrow = (row) => {
  currentAccount.value = row
  borrowForm.value.platform = row.platform
  borrowForm.value.project = ''
  borrowForm.value.purpose = '购买标书'
  borrowForm.value.returnDate = ''
  borrowForm.value.remark = ''
  showBorrowDialog.value = true
}

const handleEdit = (row) => {
  editingAccountId.value = row.id
  const raw = row.raw || {}
  createForm.value = {
    accountName: raw.accountName || row.platform || '',
    platformType: raw.platformType || 'BID_PLATFORM',
    username: raw.username || row.username || '',
    password: '' }
  showCreateDialog.value = true
}

const handleCopyPassword = async (row) => {
  let password = row.password || ''

  if (!password && true) {
    const response = await resourcesApi.accounts.getPassword(row.id)
    if (!response?.success || !response?.data?.password) {
      ElMessage.info(response?.message || '当前账号密码不可直接查看')
      return
    }
    password = response.data.password
  }

  navigator.clipboard.writeText(password || '').then(() => {
    ElMessage.success('密码已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

const handleMoreAction = async (command, row) => {
  switch (command) {
    case 'view':
      currentAccountDetail.value = row
      showDetailDialog.value = true
      {
        const response = await resourcesApi.accounts.getDetail(row.id)
        if (!response?.success) {
          ElMessage.error(response?.message || '账户详情加载失败')
          return
        }
        currentAccountDetail.value = response.data || row
      }
      break
    case 'reset':
      try {
        await ElMessageBox.confirm(`确定要重置账户"${row.platform}"的密码吗？`, '重置密码', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const resetResponse = await resourcesApi.accounts.update(row.id, { resetPassword: true })
        if (!resetResponse?.success) {
          ElMessage.error(resetResponse?.message || '重置密码失败')
          return
        }
        ElMessage.success('密码已重置')
      } catch {
        // 用户取消
      }
      break
    case 'toggle': {
      const newStatus = row.status === 'available' ? 'disabled' : 'available'
      const toggleResponse = await resourcesApi.accounts.update(row.id, { status: newStatus.toUpperCase() })
      if (!toggleResponse?.success) {
        ElMessage.error(toggleResponse?.message || '状态更新失败')
        return
      }
      await loadAccounts()
      ElMessage.success(`已${newStatus === 'available' ? '启用' : '禁用'}账户：${row.platform}`)
      break
    }
    case 'delete':
      try {
        await ElMessageBox.confirm(`确定要删除账户"${row.platform}"吗？`, '确认删除', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await resourcesApi.accounts.delete(row.id)
        if (!response?.success) {
          ElMessage.error(response?.message || '删除失败')
          return
        }
        await loadAccounts()
        ElMessage.success(`删除账户：${row.platform}`)
      } catch {
        // 用户取消
      }
      break
  }
}

const handleCreate = () => {
  editingAccountId.value = null
  createForm.value = {
    accountName: '',
    platformType: 'BID_PLATFORM',
    username: '',
    password: '' }
  showCreateDialog.value = true
}

const submitCreate = async () => {
  const isEditing = !!editingAccountId.value
  const payload = {
    accountName: createForm.value.accountName.trim(),
    platformType: createForm.value.platformType,
    username: createForm.value.username.trim() }

  // Password is required for create, optional for edit
  if (createForm.value.password) {
    payload.password = createForm.value.password
  }

  if (!payload.accountName || !payload.username || (!isEditing && !payload.password)) {
    ElMessage.warning('请完整填写账户信息')
    return
  }

  let response
  if (isEditing) {
    response = await resourcesApi.accounts.update(editingAccountId.value, payload)
  } else {
    payload.password = createForm.value.password
    response = await resourcesApi.accounts.create(payload)
  }

  if (!response?.success) {
    ElMessage.error(response?.message || (isEditing ? '编辑账户失败' : '新增账户失败'))
    return
  }

  await loadAccounts()
  showCreateDialog.value = false
  editingAccountId.value = null
  ElMessage.success(isEditing ? '账户已更新' : '账户已新增')
}

const handleSubmitBorrow = async () => {
  if (!currentAccount.value) return

  const payload = {
    borrowedBy: Number(userStore.currentUser?.id || 0),
    dueHours: 24
  }

  const response = await resourcesApi.accounts.borrow(currentAccount.value.id, payload)
  if (!response?.success) {
    ElMessage.error(response?.message || '借阅申请提交失败')
    return
  }

  await loadAccounts()
  ElMessage.success('借阅申请已提交')
  showBorrowDialog.value = false
}

// 切换密码可见性
const togglePasswordVisibility = (accountId) => {
  passwordVisible.value[accountId] = !passwordVisible.value[accountId]
}

onMounted(() => {
  loadAccounts()
})
</script>

<style scoped lang="scss">
.account-page {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.platform-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.platform-icon {
  color: #409eff;
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.action-buttons .el-button {
  padding: 4px;
}

.action-buttons .el-button.is-disabled {
  opacity: 0.5;
}

/* 密码单元格样式 */
.password-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.password-text {
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .account-page {
    padding: 12px;
  }

  .search-card {
    margin-bottom: 12px;
  }

  .search-card :deep(.el-form) {
    display: block;
  }

  .search-card :deep(.el-form-item) {
    display: block;
    margin-right: 0;
    margin-bottom: 12px;
  }

  .search-card :deep(.el-input),
  .search-card :deep(.el-select) {
    width: 100% !important;
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

  /* 头部按钮移动端优化 */
  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .card-header .el-button {
    width: 100%;
  }

  /* 对话框移动端优化 */
  :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }

  :deep(.el-dialog__body) {
    padding: 16px;
  }

  /* 分页移动端优化 */
  .pagination-wrapper {
    justify-content: center;
  }

  .pagination-wrapper :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }

  .pagination-wrapper :deep(.el-pagination__sizes),
  .pagination-wrapper :deep(.el-pagination__jump) {
    display: none;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .el-button {
    min-height: 44px;
  }
}
</style>
