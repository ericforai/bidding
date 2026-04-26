import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DocVerificationWorkbench from '../DocVerificationWorkbench.vue'

describe('DocVerificationWorkbench', () => {
  it('mounts with basic data', () => {
    const wrapper = mount(DocVerificationWorkbench, {
      props: {
        data: { projectName: 'Test' },
        schema: { groups: [] }
      },
      global: {
        stubs: {
          'el-row': true,
          'el-col': true,
          'el-form': true,
          'el-card': true,
          'el-button': true,
          'el-tag': true,
          'el-icon': true,
          'Location': true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
