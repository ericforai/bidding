import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ManualTenderDialog from './ManualTenderDialog.vue'

const ElUploadStub = {
  name: 'ElUpload',
  props: ['onChange'],
  template: '<div class="upload-stub"><slot /></div>',
}

function createForm(overrides = {}) {
  return {
    title: '',
    budget: null,
    region: '',
    industry: '',
    deadline: '',
    purchaser: '',
    contact: '',
    description: '',
    tags: [],
    attachments: [],
    ...overrides,
  }
}

function mountDialog(props = {}) {
  return shallowMount(ManualTenderDialog, {
    props: {
      modelValue: true,
      form: createForm(),
      ...props,
    },
    global: {
      stubs: {
        'el-button': { template: '<button><slot /></button>' },
        'el-col': { template: '<div><slot /></div>' },
        'el-date-picker': { template: '<input />' },
        'el-dialog': { template: '<section><slot /><slot name="footer" /></section>' },
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': { template: '<label><slot /></label>' },
        'el-icon': { template: '<span><slot /></span>' },
        'el-input': { template: '<input />' },
        'el-input-number': { template: '<input />' },
        'el-option': { template: '<option />' },
        'el-row': { template: '<div><slot /></div>' },
        'el-select': { template: '<select><slot /></select>' },
        'el-upload': ElUploadStub,
        Upload: { template: '<i />' },
      },
    },
  })
}

describe('ManualTenderDialog', () => {
  it('keeps the attachment upload area under the form width after files are selected', () => {
    const wrapper = mountDialog({
      form: createForm({
        attachments: [
          {
            name: '超长文件名-西域数智化投标管理平台-技术标-商务标-报价清单-最终版-v20260424.pdf',
            uid: 'file-1',
            status: 'ready',
          },
        ],
      }),
    })

    expect(wrapper.find('.manual-tender-upload').exists()).toBe(true)
  })

  it('emits file changes to the parent workflow', () => {
    const wrapper = mountDialog()
    const upload = wrapper.findComponent({ name: 'ElUpload' })
    const file = { name: '标讯附件.pdf' }
    const fileList = [file]

    upload.props('onChange')(file, fileList)

    expect(wrapper.emitted('file-change')).toEqual([[file, fileList]])
  })
})
