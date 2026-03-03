<template>
  <div class="bar-site-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">站点台账</h2>
        <p class="page-subtitle">管理招投标网站账号、UK、找回SOP等资产</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/resource/bar')">
          <el-icon><Back /></el-icon>
          返回检查
        </el-button>
        <el-button type="primary" @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>
          新增站点
        </el-button>
        <el-button @click="handleImport">
          <el-icon><Upload /></el-icon>
          导入
        </el-button>
      </div>
    </div>

    <!-- 筛选卡片 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="地区">
          <el-select v-model="filterForm.region" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="全国" value="全国" />
            <el-option label="华北" value="华北" />
            <el-option label="华东" value="华东" />
            <el-option label="华南" value="华南" />
            <el-option label="西南" value="西南" />
          </el-select>
        </el-form-item>
        <el-form-item label="行业">
          <el-select v-model="filterForm.industry" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="政府" value="政府" />
            <el-option label="央企" value="央企" />
            <el-option label="建设" value="建设" />
            <el-option label="军队" value="军队" />
          </el-select>
        </el-form-item>
        <el-form-item label="登录方式">
          <el-select v-model="filterForm.loginType" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="密码登录" value="password" />
            <el-option label="CA登录" value="ca" />
            <el-option label="密码+CA" value="both" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="正常" value="active" />
            <el-option label="异常" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险">
          <el-select v-model="filterForm.hasRisk" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="有风险" value="true" />
            <el-option label="无风险" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 站点列表 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="filteredSites" stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column label="平台名称" min-width="240">
          <template #default="{ row }">
            <div class="site-name-cell">
              <div class="name">{{ row.name }}</div>
              <div class="url">{{ row.url }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="地区" width="80" />
        <el-table-column prop="industry" label="行业" width="80" />
        <el-table-column label="登录方式" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getLoginTypeText(row.loginType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号数" width="80" align="center">
          <template #default="{ row }">
            {{ row.accounts?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="UK数" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.uks && row.uks.length > 0">
              {{ row.uks.length }} ({{ row.uks.filter(u => u.status === 'available').length }})
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <div v-if="row.status === 'active'" class="status-dot status-active"></div>
            <div v-else class="status-dot status-inactive"></div>
          </template>
        </el-table-column>
        <el-table-column label="风险" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.hasRisk" type="warning" size="small">风险</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastVerifyTime" label="最近验证" width="110" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="查看详情" placement="top">
                <el-button
                  :icon="View"
                  circle
                  size="small"
                  type="primary"
                  @click="goToDetail(row.id)"
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
              <el-tooltip content="访问网站" placement="top">
                <el-button
                  :icon="Link"
                  circle
                  size="small"
                  type="success"
                  @click="handleVisitSite(row)"
                />
              </el-tooltip>
              <el-dropdown trigger="click" @command="(cmd) => handleMoreAction(cmd, row)">
                <el-button :icon="MoreFilled" circle size="small" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="verify" :icon="Setting">
                      立即验证
                    </el-dropdown-item>
                    <el-dropdown-item command="copy" :icon="CopyDocument">
                      复制站点信息
                    </el-dropdown-item>
                    <el-dropdown-item command="toggle" :icon="View">
                      {{ row.status === 'active' ? '禁用站点' : '启用站点' }}
                    </el-dropdown-item>
                    <el-dropdown-item divided command="delete" :icon="Delete" style="color: #f56c6c">
                      删除站点
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="showAddDialog"
      :title="editingSite ? '编辑站点' : '新增站点'"
      width="600px"
    >
      <el-form :model="siteForm" :rules="siteRules" ref="siteFormRef" label-width="100px">
        <el-form-item label="站点名称" prop="name">
          <el-input v-model="siteForm.name" placeholder="请输入站点名称" />
        </el-form-item>
        <el-form-item label="网址" prop="url">
          <el-input v-model="siteForm.url" placeholder="请输入网址，如 http://example.com" />
        </el-form-item>
        <el-form-item label="地区" prop="region">
          <el-select v-model="siteForm.region" placeholder="请选择地区">
            <el-option label="全国" value="全国" />
            <el-option label="华北" value="华北" />
            <el-option label="华东" value="华东" />
            <el-option label="华南" value="华南" />
            <el-option label="西南" value="西南" />
          </el-select>
        </el-form-item>
        <el-form-item label="行业" prop="industry">
          <el-select v-model="siteForm.industry" placeholder="请选择行业">
            <el-option label="政府" value="政府" />
            <el-option label="央企" value="央企" />
            <el-option label="建设" value="建设" />
            <el-option label="军队" value="军队" />
          </el-select>
        </el-form-item>
        <el-form-item label="站点类型" prop="siteType">
          <el-input v-model="siteForm.siteType" placeholder="如 公共资源交易中心" />
        </el-form-item>
        <el-form-item label="登录方式" prop="loginType">
          <el-radio-group v-model="siteForm.loginType">
            <el-radio value="password">密码登录</el-radio>
            <el-radio value="ca">CA登录</el-radio>
            <el-radio value="both">密码+CA</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="siteForm.remark" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSite">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useBarStore } from '@/stores/bar'
import {
  Back, Plus, Upload, Search, RefreshLeft, View, Edit, Delete, CopyDocument, MoreFilled, Link, Setting
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const barStore = useBarStore()

const loading = ref(false)
const showAddDialog = ref(false)
const editingSite = ref(null)
const siteFormRef = ref(null)

const filterForm = ref({
  region: '',
  industry: '',
  loginType: '',
  status: '',
  hasRisk: ''
})

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const siteForm = ref({
  name: '',
  url: '',
  region: '',
  industry: '',
  siteType: '',
  loginType: 'both',
  remark: ''
})

const siteRules = {
  name: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  url: [
    { required: true, message: '请输入网址', trigger: 'blur' },
    { type: 'url', message: '请输入正确的网址格式', trigger: 'blur' }
  ],
  region: [{ required: true, message: '请选择地区', trigger: 'change' }],
  industry: [{ required: true, message: '请选择行业', trigger: 'change' }],
  loginType: [{ required: true, message: '请选择登录方式', trigger: 'change' }]
}

// 过滤后的站点列表
const filteredSites = computed(() => {
  let result = [...barStore.sites]

  if (filterForm.value.region) {
    result = result.filter(s => s.region === filterForm.value.region)
  }
  if (filterForm.value.industry) {
    result = result.filter(s => s.industry === filterForm.value.industry)
  }
  if (filterForm.value.loginType) {
    result = result.filter(s => s.loginType === filterForm.value.loginType)
  }
  if (filterForm.value.status) {
    result = result.filter(s => s.status === filterForm.value.status)
  }
  if (filterForm.value.hasRisk !== '') {
    const hasRisk = filterForm.value.hasRisk === 'true'
    result = result.filter(s => s.hasRisk === hasRisk)
  }

  pagination.value.total = result.length

  const start = (pagination.value.page - 1) * pagination.value.pageSize
  const end = start + pagination.value.pageSize
  return result.slice(start, end)
})

const getLoginTypeText = (type) => {
  const map = {
    'password': '密码',
    'ca': 'CA',
    'both': '密码+CA'
  }
  return map[type] || type
}

const handleFilter = () => {
  pagination.value.page = 1
}

const handleReset = () => {
  filterForm.value = {
    region: '',
    industry: '',
    loginType: '',
    status: '',
    hasRisk: ''
  }
  pagination.value.page = 1
}

const handleSizeChange = () => {
  pagination.value.page = 1
}

const handlePageChange = () => {}

const goToDetail = (id) => {
  router.push(`/resource/bar/site/${id}`)
}

const handleEdit = (site) => {
  editingSite.value = site
  siteForm.value = { ...site }
  showAddDialog.value = true
}

const handleDelete = async (site) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除站点"${site.name}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await barStore.deleteSite(site.id)
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}

const handleSaveSite = async () => {
  try {
    await siteFormRef.value.validate()

    if (editingSite.value) {
      await barStore.updateSite(editingSite.value.id, siteForm.value)
      ElMessage.success('更新成功')
    } else {
      await barStore.createSite(siteForm.value)
      ElMessage.success('添加成功')
    }

    showAddDialog.value = false
    editingSite.value = null
    siteForm.value = {
      name: '',
      url: '',
      region: '',
      industry: '',
      siteType: '',
      loginType: 'both',
      remark: ''
    }
  } catch (error) {
    // 表单验证失败
  }
}

const handleImport = () => {
  ElMessage.info('导入功能开发中，敬请期待')
}

const handleVisitSite = (site) => {
  window.open(site.url, '_blank')
}

const handleMoreAction = async (command, site) => {
  switch (command) {
    case 'verify':
      ElMessage.success(`正在验证站点"${site.name}"...`)
      // 模拟验证
      setTimeout(() => {
        site.lastVerifyTime = new Date().toLocaleDateString()
        site.status = 'active'
        site.hasRisk = false
        ElMessage.success('验证完成，站点状态正常')
      }, 1000)
      break
    case 'copy':
      const siteInfo = `站点名称：${site.name}\n网址：${site.url}\n地区：${site.region}\n行业：${site.industry}`
      navigator.clipboard.writeText(siteInfo).then(() => {
        ElMessage.success('站点信息已复制到剪贴板')
      }).catch(() => {
        ElMessage.error('复制失败')
      })
      break
    case 'toggle':
      const newStatus = site.status === 'active' ? 'inactive' : 'active'
      site.status = newStatus
      ElMessage.success(`站点已${newStatus === 'active' ? '启用' : '禁用'}`)
      break
    case 'delete':
      await handleDelete(site)
      break
  }
}

onMounted(async () => {
  loading.value = true
  await barStore.getSites()
  loading.value = false
})
</script>

<style scoped>
.bar-site-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #1a1a1a;
}

.page-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-form {
  margin-bottom: 0;
}

.filter-form .el-form-item {
  margin-bottom: 12px;
}

.table-card {
  min-height: 400px;
}

.site-name-cell .name {
  font-weight: 500;
  color: #303133;
  word-break: break-word;
}

.site-name-cell .url {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  word-break: break-all;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin: 0 auto;
}

.status-active {
  background: #67c23a;
}

.status-inactive {
  background: #f56c6c;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 操作列按钮横向排列 */
:deep(.el-table__cell) .el-button {
  margin-right: 4px;
}
</style>
