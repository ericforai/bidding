// Regression: ISSUE-001 — clicking the expense apply button did not open the dialog
// Found by /qa on 2026-04-19
// Report: .gstack/qa-reports/qa-report-localhost-2026-04-19.md

import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick, ref } from 'vue'
import { mount } from '@vue/test-utils'

const showApplyDialog = ref(false)

vi.mock('./expense/useExpensePage.js', () => ({
  useExpensePage: () => ({
    searchForm: ref({ project: '', type: '', status: '' }),
    filteredFees: ref([]),
    displayedApprovalRecords: ref([]),
    depositTrackingList: ref([]),
    overdueCount: ref(0),
    totalPaid: ref('0.00'),
    totalPending: ref('0.00'),
    depositCount: ref(0),
    warningCount: ref(0),
    availableProjects: ref([]),
    showApplyDialog,
    showRemindDialog: ref(false),
    showApprovalDialog: ref(false),
    showDetailDialog: ref(false),
    applyForm: ref({ type: '保证金', project: '', amount: 0, remark: '', expectedReturnDate: '' }),
    approvalForm: ref({ result: 'approved', comment: '' }),
    currentRemindItem: ref(null),
    currentApprovalItem: ref(null),
    currentExpenseDetail: ref(null),
    handleSearch: vi.fn(),
    handleReset: vi.fn(),
    handleExport: vi.fn(),
    handleDetail: vi.fn(),
    handleReturn: vi.fn(),
    handleSubmitApply: vi.fn(),
    handleRemind: vi.fn(),
    confirmRemind: vi.fn(),
    handleConfirmReturn: vi.fn(),
    handleApprove: vi.fn(),
    confirmApproval: vi.fn(),
    init: vi.fn()
  })
}))

const ExpenseLedgerCardStub = defineComponent({
  emits: ['open-apply'],
  setup(_, { emit }) {
    return () => h('button', {
      class: 'open-apply-trigger',
      onClick: () => emit('open-apply')
    }, 'open apply')
  }
})

vi.mock('./expense/components/ExpenseLedgerCard.vue', () => ({
  default: ExpenseLedgerCardStub
}))

vi.mock('./expense/components/ExpenseSearchCard.vue', () => ({
  default: defineComponent({
    name: 'ExpenseSearchCardStub',
    setup() {
      return () => h('div', { class: 'expense-search-card-stub' })
    }
  })
}))

vi.mock('./expense/components/DepositTrackingCard.vue', () => ({
  default: defineComponent({
    name: 'DepositTrackingCardStub',
    setup() {
      return () => h('div', { class: 'deposit-tracking-card-stub' })
    }
  })
}))

vi.mock('./expense/components/ApprovalRecordCard.vue', () => ({
  default: defineComponent({
    name: 'ApprovalRecordCardStub',
    setup() {
      return () => h('div', { class: 'approval-record-card-stub' })
    }
  })
}))

describe('Expense.vue regression', () => {
  it('opens the apply dialog when the ledger card emits open-apply', async () => {
    showApplyDialog.value = false

    const Expense = (await import('./Expense.vue')).default
    const wrapper = mount(Expense, {
      global: {
        stubs: {
          'el-dialog': true,
          ElDialog: true,
          'el-form': true,
          'el-form-item': true,
          'el-select': true,
          'el-option': true,
          'el-input-number': true,
          'el-date-picker': true,
          'el-input': true,
          'el-button': true,
          'el-descriptions': true,
          'el-descriptions-item': true,
          'el-divider': true,
          'el-radio-group': true,
          'el-radio': true
        }
      }
    })

    await wrapper.get('.open-apply-trigger').trigger('click')
    await nextTick()

    expect(showApplyDialog.value).toBe(true)
  })
})
