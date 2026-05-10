import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('element-plus', () => ({
  ElButton: { props: ['icon'], template: '<button><slot /></button>' },
  ElDropdown: { template: '<div><slot /><slot name="dropdown" /></div>' },
  ElDropdownItem: { template: '<div><slot /></div>' },
  ElDropdownMenu: { template: '<div><slot /></div>' },
  ElTooltip: { template: '<div><slot /></div>' },
}))

const mockDisconnect = vi.fn()
const mockObserve = vi.fn()

vi.stubGlobal('ResizeObserver', vi.fn().mockImplementation(() => ({
  observe: mockObserve,
  disconnect: mockDisconnect,
})))

import TenderActionMenu from './TenderActionMenu.vue'

function mountMenu(props = {}) {
  const rowData = props.row || { id: 1, title: '测试标讯' }
  const stubs = {
    ElButton: { template: '<button><slot /></button>' },
    ElDropdown: { template: '<div><slot /><slot name="dropdown" /></div>' },
    ElDropdownItem: { template: '<div class="dropdown-item"><slot /></div>' },
    ElDropdownMenu: { template: '<div class="dropdown-menu"><slot /></div>' },
    ElTooltip: { template: '<div><slot /></div>' },
  }

  return mount(TenderActionMenu, {
    props: {
      row: rowData,
      canManageTenders: false,
      canDeleteTenders: false,
      showAiEntry: true,
      isAdmin: false,
      ...props,
    },
    global: {
      stubs: {
        ...stubs,
        'el-button': stubs.ElButton,
        'el-dropdown': stubs.ElDropdown,
        'el-dropdown-item': stubs.ElDropdownItem,
        'el-dropdown-menu': stubs.ElDropdownMenu,
        'el-tooltip': stubs.ElTooltip,
      },
    },
  })
}

beforeEach(() => {
  mockDisconnect.mockClear()
  mockObserve.mockClear()
})

afterEach(() => {
  document.body.innerHTML = ''
})

describe('TenderActionMenu permissions', () => {
  it('hides management and delete menu items from staff users', () => {
    const wrapper = mountMenu()

    // staff 用户看不到管理相关菜单
    const dropdownHtml = wrapper.html()
    expect(dropdownHtml).not.toContain('删除')
    expect(dropdownHtml).not.toContain('编辑')
    expect(dropdownHtml).not.toContain('审核')
  })

  it('shows delete menu item for admins', () => {
    const wrapper = mountMenu({ canManageTenders: true, canDeleteTenders: true })

    // 检查 HTML 中包含删除选项
    expect(wrapper.html()).toContain('删除')
  })

  it('shows edit option when status is TRACKING', () => {
    const trackingRow = { id: 1, title: '测试', status: 'TRACKING' }
    const wrapper = mountMenu({ canManageTenders: true, row: trackingRow })

    expect(wrapper.html()).toContain('编辑')
  })

  it('shows review option for admin when status is EVALUATED', () => {
    const evaluatedRow = { id: 1, title: '测试', status: 'EVALUATED' }
    const wrapper = mountMenu({ isAdmin: true, row: evaluatedRow })

    expect(wrapper.html()).toContain('审核')
  })
})
