import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'

const {
  routerPush,
  currentUser,
  setCalendar,
  dashboardGetSummary,
  tasksGetMine,
  tasksComplete,
  alertGetUnresolved,
  alertAcknowledge,
  approvalGetPendingApprovals,
  approvalGetMyApprovals,
  approvalSubmitApproval,
  projectsGetList,
  scheduleGetOverview,
  messageSuccess,
  messageWarning,
  messageError,
  messageInfo,
} = vi.hoisted(() => ({
  routerPush: vi.fn(),
  currentUser: { id: 7, name: '小王', role: 'staff' },
  setCalendar: vi.fn(),
  dashboardGetSummary: vi.fn(),
  tasksGetMine: vi.fn(),
  tasksComplete: vi.fn(),
  alertGetUnresolved: vi.fn(),
  alertAcknowledge: vi.fn(),
  approvalGetPendingApprovals: vi.fn(),
  approvalGetMyApprovals: vi.fn(),
  approvalSubmitApproval: vi.fn(),
  projectsGetList: vi.fn(),
  scheduleGetOverview: vi.fn(),
  messageSuccess: vi.fn(),
  messageWarning: vi.fn(),
  messageError: vi.fn(),
  messageInfo: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    get currentUser() {
      return currentUser
    },
  }),
}))

vi.mock('@/stores/bidding', () => ({
  useBiddingStore: () => ({
    setCalendar,
  }),
}))

vi.mock('@/api', () => ({
  dashboardApi: {
    getSummary: dashboardGetSummary,
  },
  approvalApi: {
    getPendingApprovals: approvalGetPendingApprovals,
    getMyApprovals: approvalGetMyApprovals,
    submitApproval: approvalSubmitApproval,
  },
  projectsApi: {
    getList: projectsGetList,
  },
}))

vi.mock('@/api/modules/dashboard.js', () => ({
  tasksApi: {
    getMine: tasksGetMine,
    complete: tasksComplete,
  },
}))

vi.mock('@/api/modules/alerts.js', () => ({
  alertHistoryApi: {
    getUnresolved: alertGetUnresolved,
    acknowledge: alertAcknowledge,
  },
}))

vi.mock('@/api/modules/workbench.js', () => ({
  workbenchApi: {
    getScheduleOverview: scheduleGetOverview,
  },
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: messageSuccess,
    warning: messageWarning,
    error: messageError,
    info: messageInfo,
  },
}))

import Workbench from '@/views/Dashboard/Workbench.vue'

const passthrough = (template, props = [], emits = []) => ({
  props,
  emits,
  template,
})

const globalMountOptions = {
  directives: {
    loading: {},
  },
  stubs: {
    'el-button': passthrough(
      '<button type="button" :disabled="loading" @click="$emit(\'click\', $event)"><slot /></button>',
      ['type', 'size', 'text', 'loading', 'icon'],
      ['click']
    ),
    'el-link': passthrough(
      '<button type="button" class="el-link-stub" @click="$emit(\'click\', $event)"><slot /></button>',
      ['type', 'underline'],
      ['click']
    ),
    'el-tag': passthrough('<span class="el-tag-stub"><slot /></span>', ['type', 'size', 'effect']),
    'el-icon': passthrough('<span class="el-icon-stub"><slot /></span>', ['size']),
    'el-form': passthrough('<form><slot /></form>', ['model', 'labelWidth']),
    'el-form-item': passthrough('<label><span>{{ label }}</span><slot /></label>', ['label', 'required']),
    'el-option': passthrough('<option :value="value">{{ label }}</option>', ['label', 'value']),
    'el-select': {
      props: ['modelValue', 'placeholder', 'filterable'],
      emits: ['update:modelValue'],
      template: `
        <select
          class="el-select-stub"
          :value="modelValue ?? ''"
          @change="$emit('update:modelValue', $event.target.value)"
        >
          <slot />
        </select>
      `,
    },
    'el-date-picker': {
      props: ['modelValue', 'type', 'placeholder', 'valueFormat'],
      emits: ['update:modelValue'],
      template: `
        <input
          class="el-date-picker-stub"
          :value="modelValue"
          @input="$emit('update:modelValue', $event.target.value)"
        />
      `,
    },
    'el-input': {
      props: ['modelValue', 'type', 'rows', 'maxlength', 'showWordLimit', 'placeholder'],
      emits: ['update:modelValue'],
      template: `
        <textarea
          v-if="type === 'textarea'"
          class="el-input-stub"
          :value="modelValue"
          @input="$emit('update:modelValue', $event.target.value)"
        />
        <input
          v-else
          class="el-input-stub"
          :value="modelValue"
          @input="$emit('update:modelValue', $event.target.value)"
        />
      `,
    },
    'el-checkbox': {
      props: ['modelValue'],
      emits: ['update:modelValue', 'change'],
      template: `
        <input
          type="checkbox"
          class="el-checkbox-stub"
          :checked="modelValue"
          @change="$emit('update:modelValue', $event.target.checked); $emit('change', $event.target.checked)"
        />
      `,
    },
    'el-dialog': {
      props: ['modelValue', 'title', 'width', 'destroyOnClose'],
      emits: ['update:modelValue'],
      template: `
        <section v-if="modelValue" class="el-dialog-stub">
          <h2>{{ title }}</h2>
          <slot />
          <footer><slot name="footer" /></footer>
        </section>
      `,
    },
    'el-calendar': {
      props: ['modelValue'],
      emits: ['update:modelValue'],
      template: '<div class="el-calendar-stub"><slot name="date-cell" :data="{ date: modelValue || new Date(), day: \'2026-04-22\', viewType: \'month\' }" /></div>',
    },
    ApprovalDialog: passthrough('<div class="approval-dialog-stub" />', ['visible', 'mode', 'approvalInfo']),
  },
}

const users = {
  sales: { id: 7, name: '小王', role: 'staff' },
  manager: { id: 8, name: '张经理', role: 'manager' },
  staff: { id: 9, name: '李工', role: 'staff' },
  admin: { id: 1, name: '管理员', role: 'admin' },
}

function resetApiMocks() {
  dashboardGetSummary.mockResolvedValue({
    success: true,
    data: {
      totalBudget: 2000000,
      successRate: 48.6,
      totalTenders: 12,
      activeProjects: 4,
      pendingTasks: 3,
    },
  })
  tasksGetMine.mockResolvedValue({
    data: [
      {
        id: 501,
        title: 'API任务：完善技术方案',
        priority: 'HIGH',
        status: 'TODO',
        dueDate: '2026-04-23T10:30:00',
      },
    ],
  })
  tasksComplete.mockResolvedValue({ success: true, data: { status: 'COMPLETED' } })
  alertGetUnresolved.mockResolvedValue({
    data: [
      {
        id: 601,
        severity: 'CRITICAL',
        message: '保证金即将到期',
        status: 'ACTIVE',
        createdAt: '2026-04-21T09:00:00',
      },
    ],
  })
  alertAcknowledge.mockResolvedValue({ success: true })
  approvalGetPendingApprovals.mockResolvedValue({
    totalCount: 2,
    data: [
      {
        id: 701,
        title: '数字政府项目 - 预算审批',
        approvalType: 'expense',
        applicantDept: '销售一部',
        submitTime: '2026-04-22 09:00',
      },
    ],
  })
  approvalGetMyApprovals.mockResolvedValue({
    data: [
      {
        id: 801,
        title: '数字政府项目 - 标书支持申请',
        status: 'PENDING',
        description: '等待技术支持排期',
        submitTime: '2026-04-22 08:30',
      },
    ],
  })
  approvalSubmitApproval.mockResolvedValue({ success: true, data: { id: 901 } })
  projectsGetList.mockResolvedValue({
    success: true,
    data: [
      { id: '101', name: '数字政府项目' },
      { id: '102', projectName: '智慧园区项目' },
    ],
  })
  scheduleGetOverview.mockResolvedValue({
    data: {
      events: [
        {
          id: 301,
          eventDate: '2026-04-23',
          eventType: 'DEADLINE',
          title: '数字政府项目截标',
          projectId: 101,
          isUrgent: true,
        },
      ],
    },
  })
}

async function mountWorkbench(user = users.sales) {
  Object.assign(currentUser, user)
  const wrapper = mount(Workbench, {
    global: globalMountOptions,
  })
  await flushPromises()
  await nextTick()
  return wrapper
}

function findByText(wrapper, selector, text) {
  return wrapper.findAll(selector).find((item) => item.text().includes(text))
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.setSystemTime(new Date('2026-04-22T09:00:00+08:00'))
  vi.clearAllMocks()
  resetApiMocks()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('Dashboard Workbench characterization', () => {
  it('renders the role-specific first screen sections for the known demo personas', async () => {
    const sales = await mountWorkbench(users.sales)
    expect(sales.text()).toContain('上午好，小王')
    expect(sales.text()).toContain('快速发起')
    expect(sales.text()).toContain('标书支持申请')
    expect(sales.text()).toContain('重点标讯')
    sales.unmount()

    const manager = await mountWorkbench(users.manager)
    expect(manager.text()).toContain('上午好，张经理')
    expect(manager.text()).toContain('我的项目')
    expect(manager.text()).toContain('团队任务分配')
    manager.unmount()

    const staff = await mountWorkbench(users.staff)
    expect(staff.text()).toContain('上午好，李工')
    expect(staff.text()).toContain('我的任务')
    expect(staff.text()).toContain('待我评审')
    staff.unmount()

    const admin = await mountWorkbench(users.admin)
    expect(admin.text()).toContain('上午好，管理员')
    expect(admin.text()).toContain('团队绩效')
    expect(admin.text()).toContain('待审批事项')
    admin.unmount()
  })

  it('renders API-loaded summary, todos, approvals, process and calendar data on the page', async () => {
    const wrapper = await mountWorkbench(users.admin)

    expect(wrapper.text()).toContain('年度中标金额')
    expect(wrapper.text()).toContain('¥200万')
    expect(wrapper.text()).toContain('整体中标率')
    expect(wrapper.text()).toContain('48.6%')
    expect(wrapper.text()).toContain('总标讯数')
    expect(wrapper.text()).toContain('12条')
    expect(wrapper.text()).toContain('进行中项目')
    expect(wrapper.text()).toContain('4个')
    expect(wrapper.text()).toContain('API任务：完善技术方案')
    expect(wrapper.text()).toContain('保证金即将到期')
    expect(wrapper.text()).toContain('数字政府项目 - 预算审批')
    expect(wrapper.text()).toContain('数字政府项目 - 标书支持申请')
    expect(wrapper.text()).toContain('数字政府项目截标')
    expect(wrapper.text()).toContain('1 个节点')
  })

  it('routes metric cards through the dashboard drilldown contract', async () => {
    const wrapper = await mountWorkbench(users.admin)
    const revenueCard = findByText(wrapper, '.metric-card', '年度中标金额')

    await revenueCard.trigger('click')

    expect(routerPush).toHaveBeenCalledWith({
      path: '/analytics/dashboard',
      query: { drilldown: 'revenue' },
    })
  })

  it('keeps quick actions wired to support dialog and resource routes', async () => {
    const wrapper = await mountWorkbench(users.sales)

    await findByText(wrapper, '.quick-action-item', '标书支持申请').trigger('click')
    await flushPromises()
    expect(wrapper.find('.el-dialog-stub').text()).toContain('标书支持申请')
    expect(wrapper.find('.el-dialog-stub').text()).toContain('数字政府项目')

    await findByText(wrapper, '.quick-action-item', '资质/合同借阅').trigger('click')
    expect(routerPush).toHaveBeenCalledWith('/resource/contract-borrow')

    await findByText(wrapper, '.quick-action-item', '投标费用申请').trigger('click')
    expect(routerPush).toHaveBeenCalledWith('/resource/expense')
  })

  it('validates support requests and submits the current payload shape', async () => {
    const wrapper = await mountWorkbench(users.sales)

    await findByText(wrapper, '.quick-action-item', '标书支持申请').trigger('click')
    await flushPromises()
    await findByText(wrapper, '.el-dialog-stub button', '提交申请').trigger('click')

    expect(messageWarning).toHaveBeenCalledWith('请填写需求说明')
    expect(approvalSubmitApproval).not.toHaveBeenCalled()

    await wrapper.find('textarea.el-input-stub').setValue('  需要技术标评审和商务响应支持  ')
    await wrapper.find('input.el-date-picker-stub').setValue('2026-04-25T18:00:00')
    await findByText(wrapper, '.el-dialog-stub button', '提交申请').trigger('click')
    await flushPromises()

    expect(approvalSubmitApproval).toHaveBeenCalledWith({
      projectId: 101,
      projectName: '数字政府项目',
      approvalType: 'bid_support',
      title: '数字政府项目 - 标书支持申请',
      description: '需要技术标评审和商务响应支持',
      dueDate: '2026-04-25T18:00:00',
      priority: 1,
    })
    expect(messageSuccess).toHaveBeenCalledWith('标书支持申请已提交')
  })

  it('completes API-backed todo items without re-completing finished rows', async () => {
    const wrapper = await mountWorkbench(users.staff)
    const todo = findByText(wrapper, '.todo-item', 'API任务：完善技术方案')

    await todo.find('.todo-checkbox').trigger('click')
    await flushPromises()

    expect(tasksComplete).toHaveBeenCalledWith(501)
    expect(messageSuccess).toHaveBeenCalledWith('完成任务: API任务：完善技术方案')
    expect(todo.text()).toContain('API任务：完善技术方案')

    await todo.find('.todo-checkbox').trigger('click')
    await flushPromises()
    expect(tasksComplete).toHaveBeenCalledTimes(1)
  })

  it('pushes normalized calendar events into the bidding store after loading schedule overview', async () => {
    await mountWorkbench(users.manager)

    expect(scheduleGetOverview).toHaveBeenCalledWith({
      start: expect.any(Date),
      end: expect.any(Date),
      assigneeId: 8,
    })
    expect(setCalendar).toHaveBeenCalledWith([
      expect.objectContaining({
        id: 301,
        date: '2026-04-23',
        eventType: 'DEADLINE',
        type: 'deadline',
        title: '数字政府项目截标',
        projectId: 101,
        urgent: true,
      }),
    ])
  })
})
