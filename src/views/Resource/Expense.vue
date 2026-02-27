<template>
  <div class="expense-page">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="项目名称">
          <el-input v-model="searchForm.project" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="费用类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable>
            <el-option label="保证金" value="保证金" />
            <el-option label="标书费" value="标书费" />
            <el-option label="差旅费" value="差旅费" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="已支付" value="paid" />
            <el-option label="待支付" value="pending" />
            <el-option label="已退还" value="returned" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>费用台账</span>
          <div>
            <el-button type="primary" @click="showApplyDialog = true">
              <el-icon><Plus /></el-icon> 费用申请
            </el-button>
            <el-button @click="handleExport">
              <el-icon><Download /></el-icon> 导出
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-item">
            <div class="stat-value">¥{{ totalPaid }}万</div>
            <div class="stat-label">已支付总额</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-item">
            <div class="stat-value">¥{{ totalPending }}万</div>
            <div class="stat-label">待支付总额</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-item">
            <div class="stat-value">{{ depositCount }}</div>
            <div class="stat-label">保证金笔数</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-item warning">
            <div class="stat-value">{{ warningCount }}</div>
            <div class="stat-label">待退还提醒</div>
          </div>
        </el-col>
      </el-row>

      <el-table :data="filteredFees" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="project" label="项目名称" min-width="150" />
        <el-table-column prop="type" label="费用类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.type === '保证金'" type="warning">保证金</el-tag>
            <el-tag v-else-if="row.type === '标书费'" type="success">标书费</el-tag>
            <el-tag v-else-if="row.type === '差旅费'" type="info">差旅费</el-tag>
            <el-tag v-else>其他</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额(万元)" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">¥{{ row.amount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'paid'" type="success">已支付</el-tag>
            <el-tag v-else-if="row.status === 'pending'" type="warning">待支付</el-tag>
            <el-tag v-else-if="row.status === 'returned'" type="info">已退还</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="发生日期" width="120" />
        <el-table-column prop="returnDate" label="预计退还日期" width="130">
          <template #default="{ row }">
            <span v-if="row.returnDate" :class="{ 'warning-text': isReturnOverdue(row.returnDate) }">
              {{ row.returnDate }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'paid' && row.type === '保证金'" link type="success" size="small" @click="handleReturn(row)">申请退还</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 保证金跟踪卡片 -->
    <el-card class="deposit-tracking-card">
      <template #header>
        <div class="card-header">
          <span>保证金归还跟踪</span>
          <el-tag v-if="overdueCount > 0" type="danger">
            {{ overdueCount }}笔超期未退
          </el-tag>
          <el-tag v-else type="success">
            无超期
          </el-tag>
        </div>
      </template>

      <el-table :data="depositList" stripe>
        <el-table-column prop="project" label="项目名称" min-width="150" />
        <el-table-column prop="amount" label="金额(万元)" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">¥{{ row.amount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="缴纳日期" width="120" />
        <el-table-column prop="expectedReturn" label="应退日期" width="130">
          <template #default="{ row }">
            <span :class="{ 'overdue-text': isOverdue(row.expectedReturn) && row.status !== 'returned' }">
              {{ row.expectedReturn }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'returned'" type="success">已退还</el-tag>
            <el-tag v-else-if="isOverdue(row.expectedReturn)" type="danger">超期未退</el-tag>
            <el-tag v-else type="warning">待退还</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payee" label="收款人" min-width="150" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'returned'"
              link
              type="primary"
              size="small"
              @click="handleRemind(row)"
            >
              <el-icon><Bell /></el-icon> 提醒
            </el-button>
            <el-button
              v-if="row.status !== 'returned'"
              link
              type="success"
              size="small"
              @click="handleConfirmReturn(row)"
            >
              确认退还
            </el-button>
            <span v-else class="text-muted">已完成</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 费用申请对话框 -->
    <el-dialog v-model="showApplyDialog" title="费用申请" width="500px">
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="费用类型">
          <el-select v-model="applyForm.type">
            <el-option label="保证金" value="保证金" />
            <el-option label="标书费" value="标书费" />
            <el-option label="差旅费" value="差旅费" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联项目">
          <el-select v-model="applyForm.project" placeholder="请选择">
            <el-option label="某央企智慧办公平台采购" value="某央企项目" />
            <el-option label="华南电力集团集采项目" value="华南电力" />
            <el-option label="深圳地铁自动化系统" value="深圳地铁" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="applyForm.amount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="applyForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApplyDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 保证金提醒对话框 -->
    <el-dialog v-model="showRemindDialog" title="发送保证金归还提醒" width="500px">
      <div v-if="currentRemindItem" class="remind-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="项目名称">{{ currentRemindItem.project }}</el-descriptions-item>
          <el-descriptions-item label="保证金金额">¥{{ currentRemindItem.amount }}万元</el-descriptions-item>
          <el-descriptions-item label="应退日期">{{ currentRemindItem.expectedReturn }}</el-descriptions-item>
          <el-descriptions-item label="收款方">{{ currentRemindItem.payee }}</el-descriptions-item>
        </el-descriptions>
        <div class="remind-message">
          <el-divider />
          <p><strong>提醒内容：</strong></p>
          <p>{{ currentRemindItem.payee }}：</p>
          <p>您好！请及时退还{{ currentRemindItem.project }}项目保证金（金额：¥{{ currentRemindItem.amount }}万元），应退日期为{{ currentRemindItem.expectedReturn }}。</p>
          <p v-if="isOverdue(currentRemindItem.expectedReturn)" class="overdue-notice">
            <el-icon><WarningFilled /></el-icon>
            该保证金已超期{{ getOverdueDays(currentRemindItem.expectedReturn) }}天，请加急处理！
          </p>
        </div>
      </div>
      <template #footer>
        <el-button @click="showRemindDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmRemind">发送提醒</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Download, Bell, WarningFilled } from '@element-plus/icons-vue'
import { mockData } from '@/api/mock'

const searchForm = ref({
  project: '',
  type: '',
  status: ''
})

const fees = ref(mockData.fees)
const showApplyDialog = ref(false)
const showRemindDialog = ref(false)
const applyForm = ref({
  type: '保证金',
  project: '',
  amount: 0,
  remark: ''
})
const currentRemindItem = ref(null)

// 保证金列表 - 从费用中筛选保证金类型
const depositList = computed(() => {
  return fees.value
    .filter(item => item.type === '保证金')
    .map(item => ({
      ...item,
      // 应退日期根据开标结果和缴纳日期计算
      // 如果是已退还状态，应退日期就是实际退还日期
      // 如果未退还，应退日期 = 缴纳日期 + 60天（默认规则）
      expectedReturn: item.returnDate || calculateExpectedReturn(item.date),
      payee: getPayeeName(item.project)
    }))
})

// 超期未退数量
const overdueCount = computed(() => {
  return depositList.value.filter(item => {
    return item.status !== 'returned' && isOverdue(item.expectedReturn)
  }).length
})

// 根据项目名称获取收款人（模拟数据）
const getPayeeName = (project) => {
  const payeeMap = {
    '某央企项目': '某央企招标办',
    '华南电力': '华南电力集团',
    '深圳地铁': '深圳地铁集团',
    '西部云': '西部云数据中心'
  }
  return payeeMap[project] || '未知收款方'
}

// 计算应退日期（缴纳日期 + 60天）
const calculateExpectedReturn = (payDate) => {
  const date = new Date(payDate)
  date.setDate(date.getDate() + 60)
  return date.toISOString().split('T')[0]
}

// 判断是否超期
const isOverdue = (dateStr) => {
  if (!dateStr) return false
  return new Date(dateStr) < new Date()
}

// 计算超期天数
const getOverdueDays = (dateStr) => {
  if (!dateStr) return 0
  const today = new Date()
  const targetDate = new Date(dateStr)
  const diffTime = today - targetDate
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays > 0 ? diffDays : 0
}

const filteredFees = computed(() => {
  return fees.value.filter(item => {
    if (searchForm.value.project && !item.project.includes(searchForm.value.project)) return false
    if (searchForm.value.type && item.type !== searchForm.value.type) return false
    if (searchForm.value.status && item.status !== searchForm.value.status) return false
    return true
  })
})

const totalPaid = computed(() => {
  return fees.value.filter(f => f.status === 'paid').reduce((sum, f) => sum + f.amount, 0).toFixed(2)
})

const totalPending = computed(() => {
  return fees.value.filter(f => f.status === 'pending').reduce((sum, f) => sum + f.amount, 0).toFixed(2)
})

const depositCount = computed(() => {
  return fees.value.filter(f => f.type === '保证金').length
})

const warningCount = computed(() => {
  return fees.value.filter(f => f.type === '保证金' && f.status === 'paid').length
})

const isReturnOverdue = (date) => {
  return new Date(date) < new Date()
}

const handleSearch = () => {
  ElMessage.success('搜索完成')
}

const handleReset = () => {
  searchForm.value = { project: '', type: '', status: '' }
}

const handleExport = () => {
  ElMessage.success('导出成功')
}

const handleDetail = (row) => {
  ElMessage.info(`查看详情：${row.project}`)
}

const handleReturn = (row) => {
  ElMessage.success(`已提交退还申请：${row.project}`)
}

const handleSubmitApply = () => {
  ElMessage.success('费用申请已提交，等待审批')
  showApplyDialog.value = false
}

// 发送保证金归还提醒
const handleRemind = (row) => {
  currentRemindItem.value = row
  showRemindDialog.value = true
}

// 确认提醒发送
const confirmRemind = () => {
  if (currentRemindItem.value) {
    ElMessage.success(`已向${currentRemindItem.value.payee}发送保证金归还提醒`)
    showRemindDialog.value = false
    currentRemindItem.value = null
  }
}

// 确认退还
const handleConfirmReturn = (row) => {
  const index = fees.value.findIndex(f => f.id === row.id)
  if (index !== -1) {
    fees.value[index].status = 'returned'
    fees.value[index].returnDate = new Date().toISOString().split('T')[0]
    ElMessage.success(`已确认${row.project}保证金退还，金额：${row.amount}万元`)
  }
}
</script>

<style scoped lang="scss">
.expense-page {
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

.stat-row {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;

  &.warning {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  }
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.amount {
  font-weight: bold;
  color: #409eff;
}

.warning-text {
  color: #f56c6c;
}

.overdue-text {
  color: #f56c6c;
  font-weight: bold;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.deposit-tracking-card {
  margin-top: 20px;
}

.remind-content {
  .remind-message {
    padding: 10px 0;

    p {
      margin: 8px 0;
      line-height: 1.6;
    }

    .overdue-notice {
      color: #f56c6c;
      background: #fef0f0;
      padding: 10px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      gap: 8px;

      .el-icon {
        font-size: 18px;
      }
    }
  }

  /* 移动端响应式样式 */
  @media (max-width: 768px) {
    .expense-page {
      padding: 12px;
    }

    .page-header {
      margin-bottom: 12px;
    }

    .page-title {
      font-size: 20px;
    }

    /* 统计卡片移动端优化 */
    .stats-row {
      margin-bottom: 12px;
    }

    .stat-card {
      padding: 16px;
      margin-bottom: 12px;
    }

    .stat-value {
      font-size: 24px;
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

    /* 操作按钮移动端优化 */
    .card-header {
      flex-direction: column;
      gap: 12px;
      align-items: flex-start;
    }

    .card-header .el-button {
      width: 100%;
    }
  }

  /* 触摸设备优化 */
  @media (hover: none) and (pointer: coarse) {
    .stat-card {
      min-height: 80px;
    }

    .stat-card:active {
      background: #f5f7fa;
    }

    .el-button {
      min-height: 44px;
    }
  }
}
</style>
