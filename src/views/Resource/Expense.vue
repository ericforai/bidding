<template>
  <div class="expense-page">
    <ExpenseSearchCard :search-form="searchForm" @search="handleSearch" @reset="handleReset" />
    <ExpenseLedgerCard
      :rows="filteredFees"
      :total-paid="totalPaid"
      :total-pending="totalPending"
      :deposit-count="depositCount"
      :warning-count="warningCount"
      @open-apply="showApplyDialog = true"
      @export="handleExport"
      @detail="handleDetail"
      @return="handleReturn"
    />
    <DepositTrackingCard
      :rows="depositTrackingList"
      :overdue-count="overdueCount"
      @remind="handleRemind"
      @confirm-return="handleConfirmReturn"
    />
    <ApprovalRecordCard :rows="displayedApprovalRecords" @approve="handleApprove" />

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
            <el-option v-for="project in availableProjects" :key="project.id" :label="project.name" :value="project.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额"><el-input-number v-model="applyForm.amount" :min="0" :precision="2" /></el-form-item>
        <el-form-item v-if="applyForm.type === '保证金'" label="预计退还">
          <el-date-picker v-model="applyForm.expectedReturnDate" type="date" value-format="YYYY-MM-DD" placeholder="选择预计退还日期" />
        </el-form-item>
        <el-form-item label="申请说明"><el-input v-model="applyForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApplyDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRemindDialog" title="发送保证金归还提醒" width="500px">
      <el-descriptions v-if="currentRemindItem" :column="1" border>
        <el-descriptions-item label="项目名称">{{ currentRemindItem.project }}</el-descriptions-item>
        <el-descriptions-item label="保证金金额">¥{{ currentRemindItem.amount }}万元</el-descriptions-item>
        <el-descriptions-item label="应退日期">{{ currentRemindItem.expectedReturnDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近提醒">{{ currentRemindItem.lastRemindedAt || '首次提醒' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="showRemindDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmRemind">发送提醒</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showApprovalDialog" title="费用审批" width="500px">
      <div v-if="currentApprovalItem">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="项目名称">{{ currentApprovalItem.project }}</el-descriptions-item>
          <el-descriptions-item label="费用类型">{{ currentApprovalItem.type }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ currentApprovalItem.amount }}万元</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentApprovalItem.applicant }}</el-descriptions-item>
          <el-descriptions-item label="申请说明">{{ currentApprovalItem.remark || '无' }}</el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <el-form :model="approvalForm" label-width="80px">
          <el-form-item label="审批结果">
            <el-radio-group v-model="approvalForm.result">
              <el-radio value="approved">通过</el-radio>
              <el-radio value="rejected">拒绝</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审批意见">
            <el-input v-model="approvalForm.comment" type="textarea" :rows="3" placeholder="请输入审批意见" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showApprovalDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmApproval">提交审批</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="费用详情" width="620px">
      <el-descriptions v-if="currentExpenseDetail" :column="1" border>
        <el-descriptions-item label="项目名称">{{ currentExpenseDetail.project }}</el-descriptions-item>
        <el-descriptions-item label="费用类型">{{ currentExpenseDetail.type }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ Number(currentExpenseDetail.amount || 0).toFixed(2) }} 万元</el-descriptions-item>
        <el-descriptions-item label="发生日期">{{ currentExpenseDetail.date || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计退还日期">{{ currentExpenseDetail.expectedReturnDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近提醒">{{ currentExpenseDetail.lastRemindedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentExpenseDetail.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">{{ currentExpenseDetail.approvalStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentExpenseDetail.createdBy || currentExpenseDetail.applicant || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ currentExpenseDetail.approvedBy || currentExpenseDetail.approver || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见">{{ currentExpenseDetail.approvalComment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ currentExpenseDetail.description || currentExpenseDetail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'

import ApprovalRecordCard from './expense/components/ApprovalRecordCard.vue'
import DepositTrackingCard from './expense/components/DepositTrackingCard.vue'
import ExpenseLedgerCard from './expense/components/ExpenseLedgerCard.vue'
import ExpenseSearchCard from './expense/components/ExpenseSearchCard.vue'
import { useExpensePage } from './expense/useExpensePage.js'

const {
  searchForm,
  filteredFees,
  displayedApprovalRecords,
  depositTrackingList,
  overdueCount,
  totalPaid,
  totalPending,
  depositCount,
  warningCount,
  availableProjects,
  showApplyDialog,
  showRemindDialog,
  showApprovalDialog,
  showDetailDialog,
  applyForm,
  approvalForm,
  currentRemindItem,
  currentApprovalItem,
  currentExpenseDetail,
  handleSearch,
  handleReset,
  handleExport,
  handleDetail,
  handleReturn,
  handleSubmitApply,
  handleRemind,
  confirmRemind,
  handleConfirmReturn,
  handleApprove,
  confirmApproval,
  init
} = useExpensePage()

onMounted(() => {
  init()
})
</script>

<style scoped lang="scss">
.expense-page {
  padding: 20px;
}

.search-card,
.table-card,
.deposit-tracking-card,
.approval-card {
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
}

.stat-item.warning {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-label,
.text-muted {
  font-size: 14px;
  opacity: 0.9;
}

.amount,
.overdue-text {
  font-weight: bold;
  color: #409eff;
}

.overdue-text {
  color: #f56c6c;
}
</style>
