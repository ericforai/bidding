import { shallowMount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

vi.mock('element-plus', () => {
  const SlotStub = { template: '<div><slot /></div>' }
  return {
    ElButton: SlotStub,
    ElDropdown: { template: '<div><slot /><slot name="dropdown" /></div>' },
    ElDropdownItem: SlotStub,
    ElDropdownMenu: SlotStub,
    ElTooltip: SlotStub,
  }
})

import TenderActionMenu from './TenderActionMenu.vue'

const row = { id: 1, title: '测试标讯' }

function mountMenu(props = {}) {
  return shallowMount(TenderActionMenu, {
    props: {
      row,
      canManageTenders: false,
      canDeleteTenders: false,
      showAiEntry: true,
      ...props,
    },
  })
}

afterEach(() => {
  document.body.innerHTML = ''
})

describe('TenderActionMenu permissions', () => {
  it('hides management and delete menu items from staff users', () => {
    const wrapper = mountMenu()

    expect(wrapper.text()).not.toContain('分发')
    expect(wrapper.text()).not.toContain('领取')
    expect(wrapper.text()).not.toContain('指派')
    expect(wrapper.text()).not.toContain('删除')
  })

  it('shows management menu items but not delete for managers', () => {
    const wrapper = mountMenu({ canManageTenders: true })

    expect(wrapper.text()).toContain('分发')
    expect(wrapper.text()).toContain('领取')
    expect(wrapper.text()).toContain('指派')
    expect(wrapper.text()).not.toContain('删除')
  })

  it('shows delete menu item for admins', () => {
    const wrapper = mountMenu({ canManageTenders: true, canDeleteTenders: true })

    expect(wrapper.text()).toContain('删除')
  })
})
