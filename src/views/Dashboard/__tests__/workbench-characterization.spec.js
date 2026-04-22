import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  findByText,
  mocks,
  mountWorkbench,
  refreshWorkbench,
  resetApiMocks,
  users,
} from './workbench-characterization.fixture.js'

beforeEach(() => {
  vi.useFakeTimers()
  vi.setSystemTime(new Date('2026-04-22T09:00:00'))
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
    expect(wrapper.text()).toContain('本月节点1')
  })

  it('routes metric cards through the dashboard drilldown contract', async () => {
    const wrapper = await mountWorkbench(users.admin)
    const revenueCard = findByText(wrapper, '.metric-card', '年度中标金额')

    await revenueCard.trigger('click')

    expect(mocks.routerPush).toHaveBeenCalledWith({
      path: '/analytics/dashboard',
      query: { drilldown: 'revenue' },
    })
  })

  it('keeps quick actions wired to support dialog and resource routes', async () => {
    const wrapper = await mountWorkbench(users.sales)

    await findByText(wrapper, '.quick-action-item', '标书支持申请').trigger('click')
    await refreshWorkbench(wrapper)
    expect(wrapper.vm.supportRequestDialogVisible).toBe(true)
    expect(wrapper.vm.supportRequestProjects).toContainEqual(expect.objectContaining({ name: '数字政府项目' }))

    await findByText(wrapper, '.quick-action-item', '资质/合同借阅').trigger('click')
    expect(mocks.routerPush).toHaveBeenCalledWith('/resource/contract-borrow')

    await findByText(wrapper, '.quick-action-item', '投标费用申请').trigger('click')
    expect(mocks.routerPush).toHaveBeenCalledWith('/resource/expense')
  })

  it('validates support requests and submits the current payload shape', async () => {
    const wrapper = await mountWorkbench(users.sales)

    await findByText(wrapper, '.quick-action-item', '标书支持申请').trigger('click')
    await refreshWorkbench(wrapper)
    await wrapper.vm.submitSupportRequest()

    expect(mocks.messageWarning).toHaveBeenCalledWith('请填写需求说明')
    expect(mocks.approvalSubmitApproval).not.toHaveBeenCalled()

    wrapper.vm.supportRequestForm.description = '  需要技术标评审和商务响应支持  '
    wrapper.vm.supportRequestForm.dueDate = '2026-04-25T18:00:00'
    await wrapper.vm.submitSupportRequest()

    expect(mocks.approvalSubmitApproval).toHaveBeenCalledWith({
      projectId: 101,
      projectName: '数字政府项目',
      approvalType: 'bid_support',
      title: '数字政府项目 - 标书支持申请',
      description: '需要技术标评审和商务响应支持',
      dueDate: '2026-04-25T18:00:00',
      priority: 1,
    })
    expect(mocks.messageSuccess).toHaveBeenCalledWith('标书支持申请已提交')
  })

  it('completes API-backed todo items without re-completing finished rows', async () => {
    const wrapper = await mountWorkbench(users.staff)
    const todo = findByText(wrapper, '.todo-item', 'API任务：完善技术方案')

    await todo.find('.todo-checkbox').trigger('click')

    expect(mocks.tasksComplete).toHaveBeenCalledWith(501)
    expect(mocks.messageSuccess).toHaveBeenCalledWith('完成任务: API任务：完善技术方案')
    expect(todo.text()).toContain('API任务：完善技术方案')

    await todo.find('.todo-checkbox').trigger('click')
    expect(mocks.tasksComplete).toHaveBeenCalledTimes(1)
  })

  it('pushes normalized calendar events into the bidding store after loading schedule overview', async () => {
    await mountWorkbench(users.manager)

    expect(mocks.scheduleGetOverview).toHaveBeenCalledWith({
      start: expect.any(Date),
      end: expect.any(Date),
      assigneeId: 8,
    })
    expect(mocks.setCalendar).toHaveBeenCalledWith([
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
