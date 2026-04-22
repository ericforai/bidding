import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BiddingPageHeader from './BiddingPageHeader.vue'

function mountHeader(props = {}) {
  return shallowMount(BiddingPageHeader, {
    props: {
      customerOpportunityEnabled: true,
      canCreateTender: false,
      canSyncExternalSource: false,
      fetchingTenders: false,
      ...props,
    },
    global: {
      stubs: {
        'el-button': { template: '<button><slot /></button>' },
        'el-icon': { template: '<span><slot /></span>' },
      },
    },
  })
}

describe('BiddingPageHeader permissions', () => {
  it('keeps source sync hidden for staff users', () => {
    const wrapper = mountHeader()

    expect(wrapper.text()).toContain('客户商机中心')
    expect(wrapper.text()).not.toContain('标讯源配置')
    expect(wrapper.text()).not.toContain('一键获取标讯')
    expect(wrapper.text()).not.toContain('人工录入')
  })

  it('shows manual create but not source sync for managers', () => {
    const wrapper = mountHeader({ canCreateTender: true })

    expect(wrapper.text()).toContain('人工录入')
    expect(wrapper.text()).not.toContain('标讯源配置')
    expect(wrapper.text()).not.toContain('一键获取标讯')
  })

  it('shows source sync and manual create for admins', () => {
    const wrapper = mountHeader({ canCreateTender: true, canSyncExternalSource: true })

    expect(wrapper.text()).toContain('标讯源配置')
    expect(wrapper.text()).toContain('一键获取标讯')
    expect(wrapper.text()).toContain('人工录入')
  })
})
