// Input: dynamic workflow form component and Element Plus stubs
// Output: schema-driven validation and submit behavior coverage
// Pos: src/components/common/ - Common component unit tests

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DynamicWorkflowForm from './DynamicWorkflowForm.vue'

const elementStubs = {
  'el-form': { template: '<form><slot /></form>' },
  'el-form-item': { props: ['label'], template: '<label><span>{{ label }}</span><slot /></label>' },
  'el-input': { template: '<input />' },
  'el-input-number': { template: '<input />' },
  'el-date-picker': { template: '<input />' },
  'el-select': { template: '<select><slot /></select>' },
  'el-option': { template: '<option />' },
  'el-alert': { template: '<div />' }
}

const schema = {
  fields: [
    { key: 'borrower', label: '借用人', type: 'text', required: true },
    { key: 'purpose', label: '用途', type: 'textarea', required: true },
    { key: 'expectedReturnDate', label: '预计归还', type: 'date', required: true }
  ]
}

describe('DynamicWorkflowForm', () => {
  it('validates required fields from schema', () => {
    const wrapper = mount(DynamicWorkflowForm, {
      props: { schema, modelValue: {} },
      global: { stubs: elementStubs }
    })

    expect(wrapper.vm.validate()).toBe('请填写借用人')
  })

  it('emits submit only when required fields are present', async () => {
    const wrapper = mount(DynamicWorkflowForm, {
      props: { schema, modelValue: { borrower: '小王', purpose: '投标', expectedReturnDate: '2026-05-10' } },
      global: { stubs: elementStubs }
    })

    await wrapper.vm.submit()

    expect(wrapper.emitted('submit')?.[0]?.[0]).toMatchObject({ borrower: '小王', purpose: '投标' })
  })
})
