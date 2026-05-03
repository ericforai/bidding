import { mount, flushPromises } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import { reactive } from 'vue'
import TaskForm from './TaskForm.vue'

vi.mock('@/api/modules/taskStatusDict.js', () => ({
  taskStatusDictApi: {
    list: vi.fn().mockResolvedValue({ success: true, data: [
      { code: 'TODO', name: '待办', category: 'OPEN', color: '#909399', sortOrder: 10, initial: true, terminal: false },
      { code: 'IN_PROGRESS', name: '进行中', category: 'IN_PROGRESS', color: '#409eff', sortOrder: 20, initial: false, terminal: false },
      { code: 'COMPLETED', name: '已完成', category: 'CLOSED', color: '#67c23a', sortOrder: 40, initial: false, terminal: true },
    ]})
  }
}))

// Mutable mock store so individual tests can set taskExtendedFields before mounting.
const mockStoreState = reactive({
  taskExtendedFields: [],
  taskExtendedFieldsLoaded: false,
  loadTaskExtendedFields: vi.fn(async () => mockStoreState.taskExtendedFields),
})

vi.mock('@/stores/project', () => ({
  useProjectStore: () => mockStoreState,
}))

function setExtendedFields(list) {
  mockStoreState.taskExtendedFields = list
  mockStoreState.taskExtendedFieldsLoaded = true
}

// Local stubs: keep labels visible in text() and thread the :disabled prop through el-form.
const globalStubs = {
  ElForm: {
    name: 'ElForm',
    props: ['model', 'labelWidth', 'disabled'],
    template: '<form><slot /></form>',
  },
  ElFormItem: {
    props: ['label', 'required'],
    template: '<div class="form-item"><label>{{ label }}</label><slot /></div>',
  },
  ElInput: {
    props: ['modelValue', 'type', 'rows', 'placeholder'],
    emits: ['update:modelValue'],
    template: '<input class="el-input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  },
  ElDatePicker: {
    props: ['modelValue', 'type', 'valueFormat'],
    emits: ['update:modelValue'],
    template: '<input class="el-date-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  },
  ElInputNumber: {
    props: ['modelValue', 'min', 'max'],
    emits: ['update:modelValue'],
    template: '<input class="el-input-number-stub" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
  },
  ElSelect: {
    props: ['modelValue', 'loading'],
    emits: ['update:modelValue'],
    template: '<select class="el-select-stub" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
  },
  ElOption: {
    props: ['label', 'value'],
    template: '<option :value="value">{{ label }}</option>',
  },
  ElAlert: {
    props: ['title', 'type', 'closable'],
    template: '<div class="el-alert-stub">{{ title }}</div>',
  },
  ElDivider: {
    template: '<div class="el-divider-stub"><slot /></div>',
  },
}

describe('TaskForm', () => {
  beforeEach(() => {
    // Reset extended fields before each test — avoids leakage across cases.
    mockStoreState.taskExtendedFields = []
    mockStoreState.taskExtendedFieldsLoaded = false
    mockStoreState.loadTaskExtendedFields.mockClear()
  })

  it('renders system fields', async () => {
    const wrapper = mount(TaskForm, {
      props: { mode: 'create', modelValue: {} },
      global: { stubs: globalStubs },
    })
    await flushPromises()
    const text = wrapper.text()
    expect(text).toContain('任务名称')
    expect(text).toContain('详细描述')
    expect(text).toContain('负责人')
    expect(text).toContain('截止日期')
    expect(text).toContain('优先级')
    expect(text).toContain('状态')
  })

  it('submit() returns {valid:false} when name is empty', async () => {
    const wrapper = mount(TaskForm, {
      props: { mode: 'create', modelValue: {} },
      global: { stubs: globalStubs },
    })
    await flushPromises()
    const r = wrapper.vm.submit()
    expect(r.valid).toBe(false)
  })

  it('submit() returns {valid:true, data} when name provided', async () => {
    const wrapper = mount(TaskForm, {
      props: { mode: 'create', modelValue: { name: 'X' } },
      global: { stubs: globalStubs },
    })
    await flushPromises()
    const r = wrapper.vm.submit()
    expect(r.valid).toBe(true)
    expect(r.data.name).toBe('X')
  })

  it('defaults status to dict.initial code on mount when empty', async () => {
    const wrapper = mount(TaskForm, {
      props: { mode: 'create', modelValue: {} },
      global: { stubs: globalStubs },
    })
    await flushPromises()
    wrapper.vm.submit()
    const last = wrapper.emitted('update:modelValue')?.slice(-1)?.[0]?.[0]
    expect(last?.status).toBe('TODO')
  })

  it('preserves modelValue.status when provided (edit mode)', async () => {
    const wrapper = mount(TaskForm, {
      props: { mode: 'edit', modelValue: { name: 'X', status: 'IN_PROGRESS' } },
      global: { stubs: globalStubs },
    })
    await flushPromises()
    const r = wrapper.vm.submit()
    expect(r.data.status).toBe('IN_PROGRESS')
  })

  it('view mode disables the form', async () => {
    const wrapper = mount(TaskForm, {
      props: { mode: 'view', modelValue: { name: 'X' } },
      global: { stubs: globalStubs },
    })
    await flushPromises()
    const form = wrapper.findComponent({ name: 'ElForm' })
    expect(form.props('disabled')).toBe(true)
  })

  it('does not render extended fields section when store has none', async () => {
    // Default beforeEach sets taskExtendedFields to [].
    const wrapper = mount(TaskForm, {
      props: { mode: 'create', modelValue: {} },
      global: { stubs: globalStubs },
    })
    await flushPromises()
    expect(wrapper.findComponent({ name: 'DynamicFormRenderer' }).exists()).toBe(false)
    expect(wrapper.find('.el-divider-stub').exists()).toBe(false)
  })

  it('renders extended fields when store has them + prefills from modelValue', async () => {
    setExtendedFields([
      { key: 'tender_chapter', label: '招标章节号', fieldType: 'text', required: false, placeholder: '', options: null },
      { key: 'priority_level', label: '优先级别', fieldType: 'select', required: false, placeholder: '', options: [{ label: '高', value: 'high' }] },
    ])
    const wrapper = mount(TaskForm, {
      props: { mode: 'edit', modelValue: { name: 'X', status: 'TODO', extendedFields: { tender_chapter: 'Ch.3' } } },
      global: { stubs: globalStubs },
    })
    await flushPromises()
    expect(wrapper.findComponent({ name: 'DynamicFormRenderer' }).exists()).toBe(true)
    expect(wrapper.find('.el-divider-stub').exists()).toBe(true)
    const r = wrapper.vm.submit()
    expect(r.valid).toBe(true)
    expect(r.data.extendedFields?.tender_chapter).toBe('Ch.3')
  })

  it('submit() returns invalid when extended required field is empty', async () => {
    setExtendedFields([
      { key: 'tender_chapter', label: '招标章节号', fieldType: 'text', required: true, placeholder: '', options: null },
    ])
    const wrapper = mount(TaskForm, {
      props: { mode: 'create', modelValue: { name: 'X', extendedFields: {} } },
      global: { stubs: globalStubs },
    })
    await flushPromises()
    const r = wrapper.vm.submit()
    expect(r.valid).toBe(false)
    expect(r.message).toContain('招标章节号')
  })
})
