<template>
  <el-dialog
    v-model="visible"
    title="合规雷达 - 废标风险检查"
    :width="900"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>检查中...</span>
    </div>

    <div v-else-if="data" class="compliance-check">
      <!-- 风险等级总览 -->
      <section class="risk-overview">
        <el-alert
          :type="data.riskLevel === 'error' ? 'error' : data.riskLevel === 'warning' ? 'warning' : 'success'"
          :closable="false"
          show-icon
        >
          <template #title>
            <span class="risk-title">
              检测到
              <strong :class="data.highRiskCount > 0 ? 'risk-high' : 'risk-medium'">
                {{ data.highRiskCount }} 个高风险
              </strong>
              和
              <strong class="risk-medium">{{ data.mediumRiskCount }} 个中风险</strong>
              问题
            </span>
          </template>
        </el-alert>
      </section>

      <!-- 三个 Tab -->
      <el-tabs v-model="activeTab" class="compliance-tabs">
        <!-- 强制条款 -->
        <el-tab-pane name="mandatory">
          <template #label>
            <span class="tab-label">
              <el-icon><Document /></el-icon>
              强制条款
              <el-badge
                v-if="failCount(data.checks.mandatory) > 0"
                :value="failCount(data.checks.mandatory)"
                :type="failCount(data.checks.mandatory) > 0 ? 'danger' : 'info'"
                class="tab-badge"
              />
            </span>
          </template>
          <el-table
            :data="data.checks.mandatory"
            stripe
            border
            :row-class-name="getTableRowClass"
          >
            <el-table-column prop="item" label="检查项" width="140" />
            <el-table-column prop="requirement" label="要求" min-width="160" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 'pass' ? 'success' : 'danger'"
                  :icon="row.status === 'pass' ? 'CircleCheck' : 'CircleClose'"
                >
                  {{ row.status === 'pass' ? '通过' : '不通过' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="location" label="定位" width="120" />
            <el-table-column label="原因/备注" min-width="180">
              <template #default="{ row }">
                <span v-if="row.reason" :class="{ 'text-danger': row.status === 'fail' }">
                  {{ row.reason }}
                </span>
                <span v-else class="text-secondary">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 格式检查 -->
        <el-tab-pane name="format">
          <template #label>
            <span class="tab-label">
              <el-icon><Edit /></el-icon>
              格式检查
              <el-badge
                v-if="failCount(data.checks.format) > 0"
                :value="failCount(data.checks.format)"
                :type="failCount(data.checks.format) > 0 ? 'danger' : 'info'"
                class="tab-badge"
              />
            </span>
          </template>
          <el-table
            :data="data.checks.format"
            stripe
            border
            :row-class-name="getTableRowClass"
          >
            <el-table-column prop="item" label="检查项" width="140" />
            <el-table-column prop="result" label="检查结果" min-width="160" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 'pass' ? 'success' : 'danger'"
                  :icon="row.status === 'pass' ? 'CircleCheck' : 'CircleClose'"
                >
                  {{ row.status === 'pass' ? '通过' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'fail' && row.action"
                  type="primary"
                  size="small"
                  link
                  @click="handleFix(row)"
                >
                  <el-icon><Tools /></el-icon>
                  {{ row.action }}
                </el-button>
                <span v-else class="text-secondary">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 签章资质 -->
        <el-tab-pane name="qualification">
          <template #label>
            <span class="tab-label">
              <el-icon><Lock /></el-icon>
              签章资质
              <el-badge
                v-if="expiringCount(data.checks.qualification) > 0"
                :value="expiringCount(data.checks.qualification)"
                type="warning"
                class="tab-badge"
              />
            </span>
          </template>
          <el-table
            :data="data.checks.qualification"
            stripe
            border
            :row-class-name="getTableRowClass"
          >
            <el-table-column prop="item" label="检查项" width="140" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="getStatusType(row.status)"
                  :icon="getStatusIcon(row.status)"
                >
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="expireDate" label="有效期" width="140" />
            <el-table-column label="剩余天数" width="120" align="center">
              <template #default="{ row }">
                <span :class="getDaysClass(row.expireDate)">
                  {{ getDaysRemaining(row.expireDate) }} 天
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'expiring'"
                  type="warning"
                  size="small"
                  link
                  @click="handleRenew(row)"
                >
                  <el-icon><RefreshRight /></el-icon>
                  提醒续期
                </el-button>
                <span v-else class="text-secondary">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-empty v-else description="暂无合规检查数据" />

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
      <el-button type="warning" @click="handleRecheck">
        <el-icon><Refresh /></el-icon>
        重新检查
      </el-button>
      <el-button type="primary" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出检查报告
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  Loading, Document, Edit, Lock, CircleCheck, CircleClose,
  Tools, RefreshRight, Refresh, Download
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  projectId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'recheck', 'export'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const activeTab = ref('mandatory')
const data = ref(null)

// Mock 数据
const mockData = {
  P001: {
    riskLevel: 'warning',
    highRiskCount: 1,
    mediumRiskCount: 3,
    checks: {
      mandatory: [
        {
          item: '营业执照',
          requirement: '在有效期内',
          status: 'pass',
          location: '资质文件夹'
        },
        {
          item: '案例证明',
          requirement: '至少3个类似案例',
          status: 'fail',
          location: '-',
          reason: '仅2个有效案例'
        },
        {
          item: '授权书',
          requirement: '原厂授权',
          status: 'pass',
          location: '商务文件'
        },
        {
          item: '财务报表',
          requirement: '近三年审计报告',
          status: 'pass',
          location: '财务文件夹'
        },
        {
          item: '技术负责人',
          requirement: '具备高级职称',
          status: 'fail',
          location: '人员配置表',
          reason: '目前为中级职称'
        }
      ],
      format: [
        {
          item: '页码格式',
          result: '连续页码',
          status: 'pass'
        },
        {
          item: '签章要求',
          result: '公章+法人章',
          status: 'fail',
          action: '首页缺少法人章'
        },
        {
          item: '文件命名',
          result: '按招标文件要求',
          status: 'pass'
        },
        {
          item: '装订要求',
          result: '胶装/精装',
          status: 'fail',
          action: '当前为简装'
        },
        {
          item: '份数要求',
          result: '正本1份+副本4份',
          status: 'pass'
        }
      ],
      qualification: [
        {
          item: 'CMMI证书',
          status: 'valid',
          expireDate: '2024-04-15'
        },
        {
          item: 'ISO27001',
          status: 'expiring',
          expireDate: '2024-03-30'
        },
        {
          item: 'ISO9001',
          status: 'valid',
          expireDate: '2025-06-20'
        },
        {
          item: '信息安全等级',
          status: 'expiring',
          expireDate: '2024-03-15'
        },
        {
          item: '软件企业证书',
          status: 'valid',
          expireDate: '2025-12-31'
        }
      ]
    }
  }
}

const loadData = async () => {
  if (!props.projectId) return

  loading.value = true

  // 模拟 API 调用延迟
  await new Promise(resolve => setTimeout(resolve, 1000))

  // 获取 mock 数据，实际应从 API 获取
  data.value = mockData[props.projectId] || mockData.P001

  // 计算风险数量
  updateRiskCounts()

  loading.value = false
}

const updateRiskCounts = () => {
  if (!data.value) return

  const highRisk = failCount(data.value.checks.mandatory)
  const mediumRisk = failCount(data.value.checks.format) + expiringCount(data.value.checks.qualification)

  data.value.highRiskCount = highRisk
  data.value.mediumRiskCount = mediumRisk
  data.value.riskLevel = highRisk > 0 ? 'error' : mediumRisk > 0 ? 'warning' : 'success'
}

const failCount = (items) => {
  return items?.filter(item => item.status === 'fail').length || 0
}

const expiringCount = (items) => {
  return items?.filter(item => item.status === 'expiring').length || 0
}

const getTableRowClass = ({ row }) => {
  return row.status === 'fail' || row.status === 'expiring' ? 'warning-row' : ''
}

const getStatusType = (status) => {
  const types = {
    valid: 'success',
    expiring: 'warning',
    expired: 'danger'
  }
  return types[status] || 'info'
}

const getStatusIcon = (status) => {
  const icons = {
    valid: 'CircleCheck',
    expiring: 'Warning',
    expired: 'CircleClose'
  }
  return icons[status] || 'InfoFilled'
}

const getStatusText = (status) => {
  const texts = {
    valid: '有效',
    expiring: '即将过期',
    expired: '已过期'
  }
  return texts[status] || '-'
}

const getDaysRemaining = (expireDate) => {
  const today = new Date()
  const expire = new Date(expireDate)
  const diff = expire - today
  return Math.ceil(diff / (1000 * 60 * 60 * 24))
}

const getDaysClass = (expireDate) => {
  const days = getDaysRemaining(expireDate)
  if (days < 0) return 'text-danger'
  if (days < 30) return 'text-warning'
  return 'text-success'
}

const handleClose = () => {
  visible.value = false
}

const handleRecheck = () => {
  emit('recheck')
  loadData()
  ElMessage.success('正在重新检查...')
}

const handleExport = () => {
  emit('export', data.value)
  ElMessage.success('合规检查报告导出成功')
}

const handleFix = (row) => {
  ElMessageBox.confirm(
    `确定要修复 "${row.item}" 问题吗？\n\n${row.action}`,
    '确认修复',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success(`已标记 "${row.item}" 为待修复状态`)
  }).catch(() => {
    // 取消操作
  })
}

const handleRenew = (row) => {
  ElMessage.success(`已发送 "${row.item}" 续期提醒通知`)
}

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    loadData()
  }
})
</script>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 12px;
  color: var(--el-text-color-secondary);
}

.loading-container .el-icon {
  font-size: 32px;
}

.compliance-check {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.risk-overview {
  margin-bottom: 8px;
}

.risk-title {
  font-size: 14px;
}

.risk-high {
  color: var(--el-color-danger);
  font-size: 16px;
}

.risk-medium {
  color: var(--el-color-warning);
  font-size: 16px;
}

.compliance-tabs {
  margin-top: 8px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
}

.tab-badge {
  margin-left: 4px;
}

/* 表格行样式 */
:deep(.el-table .warning-row) {
  background-color: var(--el-color-warning-light-9);
}

:deep(.el-table .warning-row:hover > td) {
  background-color: var(--el-color-warning-light-8) !important;
}

/* 文本颜色 */
.text-danger {
  color: var(--el-color-danger);
  font-weight: 500;
}

.text-warning {
  color: var(--el-color-warning);
  font-weight: 500;
}

.text-success {
  color: var(--el-color-success);
  font-weight: 500;
}

.text-secondary {
  color: var(--el-text-color-secondary);
}

/* 表格列头样式 */
:deep(.el-table__header-wrapper) {
  font-weight: 600;
}
</style>
