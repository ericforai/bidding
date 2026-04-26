import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TenderConversionWorkbench from '../../TenderConversionWorkbench.vue'

const elStubs = {
  ElRow: { template: '<div><slot /></div>' },
  ElCol: { template: '<div><slot /></div>' },
  ElCard: { template: '<div><slot /><slot name="header" /></div>' },
  ElForm: { template: '<div><slot /></div>' },
  ElFormItem: { props: ['label'], template: '<div><slot /></div>' },
  ElInput: { props: ['modelValue'], template: '<input :value="modelValue" />' },
  ElInputNumber: { props: ['modelValue'], template: '<input type="number" :value="modelValue" />' },
  ElDatePicker: { props: ['modelValue'], template: '<input type="date" :value="modelValue" />' },
  ElButton: {
    props: ['type', 'loading', 'disabled'],
    emits: ['click'],
    template: '<button @click="$emit(\'click\', $event)"><slot /></button>',
  },
  ElTag: { props: ['type', 'size', 'effect'], template: '<span class="tag"><slot /></span>' },
  ElIcon: { template: '<i><slot /></i>' },
  Location: { template: '<svg />' },
}

const minimalProject = {
  id: 'p-1',
  name: '测试项目',
  tenderCode: 'TC-001',
}

const minimalProfile = {
  projectName: '测试项目',
  purchaserName: '采购单位',
  budget: 100000,
  publishDate: null,
  deadline: null,
  items: [],
}

function mountWorkbench(propsOverride = {}) {
  return mount(TenderConversionWorkbench, {
    props: {
      requirementProfile: minimalProfile,
      markdown: '',
      validationWarnings: [],
      qualificationMatches: [],
      ...propsOverride,
    },
    global: {
      stubs: elStubs,
    },
  })
}

describe('TenderConversionWorkbench', () => {
  it('mounts with minimal props without errors', () => {
    const wrapper = mountWorkbench()
    expect(wrapper.exists()).toBe(true)
  })

  it('renders the project name from requirementProfile prop', () => {
    const wrapper = mountWorkbench()
    expect(wrapper.html()).toContain('测试项目')
  })

  it('renders empty requirement list when items is empty', () => {
    const wrapper = mountWorkbench()
    const reqItems = wrapper.findAll('.req-item')
    expect(reqItems).toHaveLength(0)
  })

  it('renders requirement items from profile.items', () => {
    const wrapper = mountWorkbench({
      requirementProfile: {
        ...minimalProfile,
        items: [
          {
            category: 'qualification',
            title: '营业执照',
            content: '需提供有效营业执照',
            mandatory: true,
            sectionPath: '第三章/资质要求',
            sourceExcerpt: '营业执照',
          },
        ],
      },
    })
    expect(wrapper.find('.req-item').exists()).toBe(true)
    expect(wrapper.text()).toContain('营业执照')
    expect(wrapper.text()).toContain('需提供有效营业执照')
  })

  it('does NOT render onerror attribute when markdown contains XSS payload', () => {
    const maliciousMarkdown = '# Title\n<img src=x onerror=alert(1)>'
    const wrapper = mountWorkbench({ markdown: maliciousMarkdown })
    expect(wrapper.html()).not.toContain('onerror')
    expect(wrapper.html()).not.toContain('src=x')
  })

  it('does NOT render script tags from malicious markdown', () => {
    const maliciousMarkdown = '<script>alert("xss")</script>'
    const wrapper = mountWorkbench({ markdown: maliciousMarkdown })
    expect(wrapper.html()).not.toContain('<script')
    expect(wrapper.html()).not.toContain('alert("xss")')
  })

  it('emits cancel when cancel button is clicked', async () => {
    const wrapper = mountWorkbench()
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('emits confirm with profile when confirm button is clicked', async () => {
    const wrapper = mountWorkbench()
    const buttons = wrapper.findAll('button')
    const confirmBtn = buttons.find((b) => b.text().includes('确认立项'))
    await confirmBtn.trigger('click')
    expect(wrapper.emitted('confirm')).toBeTruthy()
  })

  it('renders markdown content safely when given valid markdown', () => {
    const safeMarkdown = '# 招标文件\n\n**重要事项**\n\n- 条款一\n- 条款二'
    const wrapper = mountWorkbench({ markdown: safeMarkdown })
    const container = wrapper.find('.markdown-container')
    expect(container.exists()).toBe(true)
    // Should contain rendered heading and bold
    expect(container.html()).toContain('<h1')
    expect(container.html()).toContain('<strong>')
  })

  it('renders empty state for markdown when markdown prop is empty string', () => {
    const wrapper = mountWorkbench({ markdown: '' })
    const container = wrapper.find('.markdown-container')
    expect(container.exists()).toBe(true)
    // No script or onerror in empty render
    expect(container.html()).not.toContain('onerror')
    expect(container.html()).not.toContain('<script')
  })
})
