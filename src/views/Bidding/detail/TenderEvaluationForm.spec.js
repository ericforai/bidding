// Instance-level permission matrix spec for TenderEvaluationForm.
//
// Replaces the legacy role-string contract: instead of asking "is the user
// MANAGER / ADMIN?" the form is driven entirely by two booleans
// (`canFill`, `canDecide`) computed by the backend on the evaluation DTO.
//
// canFill   → user is the tender's latest assignee (PM of this tender)
// canDecide → user is the latest assigned-by (the one who assigned the tender)
//
// Decision matrix (canFill × canDecide × evaluationStatus):
//
//   canFill | canDecide | status     | form editable | save/submit | bid/abandon
//   --------|-----------|------------|---------------|-------------|------------
//   true    | *         | null/DRAFT | yes           | yes         | no
//   true    | *         | SUBMITTED  | no (RO)       | no          | (see canDecide)
//   false   | *         | any        | no (RO)       | no          | (see canDecide)
//   *       | true      | SUBMITTED  | (see canFill) | -           | yes
//   *       | true      | null/DRAFT | -             | -           | no  (eval first)
//   *       | false     | any        | -             | -           | no

import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const elMessageBox = {
  confirm: vi.fn(),
  prompt: vi.fn(),
}

const elMessage = {
  success: vi.fn(),
  warning: vi.fn(),
  error: vi.fn(),
}

vi.mock('element-plus', () => ({
  ElMessage: elMessage,
  ElMessageBox: elMessageBox,
}))

import TenderEvaluationForm from './TenderEvaluationForm.vue'

const globalStubs = {
  ElForm: {
    name: 'ElForm',
    props: ['model', 'rules', 'labelWidth', 'disabled'],
    template: '<form class="el-form-stub" :data-disabled="disabled ? \'true\' : \'false\'"><slot /></form>',
  },
  ElFormItem: {
    name: 'ElFormItem',
    props: ['label', 'prop', 'required'],
    template: '<div class="el-form-item-stub" :data-prop="prop"><label>{{ label }}</label><slot /></div>',
  },
  ElInput: {
    name: 'ElInput',
    props: ['modelValue', 'type', 'rows', 'placeholder', 'disabled', 'readonly'],
    emits: ['update:modelValue'],
    template:
      '<textarea v-if="type === \'textarea\'" class="el-input-stub" :data-disabled="disabled ? \'true\' : \'false\'" :data-readonly="readonly ? \'true\' : \'false\'" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />' +
      '<input v-else class="el-input-stub" :data-disabled="disabled ? \'true\' : \'false\'" :data-readonly="readonly ? \'true\' : \'false\'" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  },
  ElInputNumber: {
    name: 'ElInputNumber',
    props: ['modelValue', 'min', 'max', 'precision', 'disabled'],
    emits: ['update:modelValue'],
    template:
      '<input class="el-input-number-stub" type="number" :data-disabled="disabled ? \'true\' : \'false\'" :data-min="min" :data-precision="precision" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
  },
  ElDatePicker: {
    name: 'ElDatePicker',
    props: ['modelValue', 'type', 'valueFormat', 'placeholder', 'disabled'],
    emits: ['update:modelValue'],
    template:
      '<input class="el-date-picker-stub" type="date" :data-disabled="disabled ? \'true\' : \'false\'" :data-type="type" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  },
  ElSelect: {
    name: 'ElSelect',
    props: ['modelValue', 'placeholder', 'disabled'],
    emits: ['update:modelValue', 'change'],
    template:
      '<select class="el-select-stub" :data-disabled="disabled ? \'true\' : \'false\'" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value); $emit(\'change\', $event.target.value)"><slot /></select>',
  },
  ElOption: {
    name: 'ElOption',
    props: ['label', 'value'],
    template: '<option :value="value">{{ label }}</option>',
  },
  ElButton: {
    name: 'ElButton',
    props: ['type', 'disabled'],
    emits: ['click'],
    template:
      '<button type="button" :data-button-type="type" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>',
  },
}

function makeWrapper(props = {}, extra = {}) {
  return mount(TenderEvaluationForm, {
    props: {
      tenderId: 9001,
      evaluation: null,
      canFill: false,
      canDecide: false,
      ...props,
    },
    global: {
      stubs: globalStubs,
      ...(extra.global || {}),
    },
  })
}

function findButtonByText(wrapper, label) {
  const buttons = wrapper.findAll('button')
  return buttons.find((b) => b.text().trim() === label) || null
}

function makeFullPayload(overrides = {}) {
  return {
    projectBackground: '某客户采购 IT 服务',
    competitorAnalysis: '主要竞争对手为 A 公司、B 公司',
    contractPeriodStart: '2026-06-01',
    contractPeriodEnd: '2027-05-31',
    shortlistedCount: 3,
    platformServiceFee: 5000.0,
    previousQuotation: '上次报价 480 万',
    bidRecommendation: 'RECOMMEND',
    ...overrides,
  }
}

function makeEvaluation(status, overrides = {}) {
  if (status === null) return null
  return {
    evaluationStatus: status,
    ...makeFullPayload(),
    ...overrides,
  }
}

async function fillRequiredFields(wrapper, payload = makeFullPayload()) {
  const textareas = wrapper.findAll('textarea.el-input-stub')
  await textareas[0].setValue(payload.projectBackground)
  await textareas[1].setValue(payload.competitorAnalysis)
  if (textareas[2] && payload.previousQuotation !== undefined) {
    await textareas[2].setValue(payload.previousQuotation)
  }
  const dates = wrapper.findAll('input.el-date-picker-stub')
  await dates[0].setValue(payload.contractPeriodStart)
  await dates[1].setValue(payload.contractPeriodEnd)
  const numbers = wrapper.findAll('input.el-input-number-stub')
  await numbers[0].setValue(String(payload.shortlistedCount))
  await numbers[1].setValue(String(payload.platformServiceFee))
  const selects = wrapper.findAll('select.el-select-stub')
  if (selects[0] && payload.bidRecommendation !== undefined) {
    await selects[0].setValue(payload.bidRecommendation)
  }
  await flushPromises()
}

function formIsDisabled(wrapper) {
  const form = wrapper.findComponent({ name: 'ElForm' })
  return form.exists() && form.props('disabled') === true
}

describe('TenderEvaluationForm — instance-level permission matrix', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ---------- rendering / fields presence ----------

  it('renders all 7 fields regardless of permissions', () => {
    const wrapper = makeWrapper({ canFill: true, canDecide: false, evaluation: null })
    const text = wrapper.text()
    expect(text).toContain('项目背景')
    expect(text).toContain('竞争对手情况')
    expect(text).toContain('项目合同周期')
    expect(text).toContain('入围家数')
    expect(text).toContain('平台服务费')
    expect(text).toContain('上一次报价情况')
    expect(text).toContain('建议是否投标')
  })

  // ---------- matrix: form-editable axis (canFill × status) ----------

  it('canFill=true + status=null → editable + save/submit buttons + NO bid/abandon', () => {
    const wrapper = makeWrapper({ canFill: true, canDecide: false, evaluation: null })
    expect(formIsDisabled(wrapper)).toBe(false)
    expect(findButtonByText(wrapper, '保存草稿')).not.toBeNull()
    expect(findButtonByText(wrapper, '提交')).not.toBeNull()
    expect(findButtonByText(wrapper, '投标')).toBeNull()
    expect(findButtonByText(wrapper, '弃标')).toBeNull()
  })

  it('canFill=true + status=DRAFT → editable + save/submit buttons + NO bid/abandon', () => {
    const wrapper = makeWrapper({
      canFill: true, canDecide: false, evaluation: makeEvaluation('DRAFT'),
    })
    expect(formIsDisabled(wrapper)).toBe(false)
    expect(findButtonByText(wrapper, '保存草稿')).not.toBeNull()
    expect(findButtonByText(wrapper, '提交')).not.toBeNull()
    expect(findButtonByText(wrapper, '投标')).toBeNull()
    expect(findButtonByText(wrapper, '弃标')).toBeNull()
  })

  it('canFill=true + status=SUBMITTED → read-only + NO save/submit', () => {
    const wrapper = makeWrapper({
      canFill: true, canDecide: false, evaluation: makeEvaluation('SUBMITTED'),
    })
    expect(formIsDisabled(wrapper)).toBe(true)
    expect(findButtonByText(wrapper, '保存草稿')).toBeNull()
    expect(findButtonByText(wrapper, '提交')).toBeNull()
  })

  it('canFill=false + status=DRAFT → read-only + NO save/submit', () => {
    const wrapper = makeWrapper({
      canFill: false, canDecide: false, evaluation: makeEvaluation('DRAFT'),
    })
    expect(formIsDisabled(wrapper)).toBe(true)
    expect(findButtonByText(wrapper, '保存草稿')).toBeNull()
    expect(findButtonByText(wrapper, '提交')).toBeNull()
  })

  it('canFill=false + status=null → read-only + NO save/submit (un-assigned tender)', () => {
    const wrapper = makeWrapper({
      canFill: false, canDecide: false, evaluation: null,
    })
    expect(formIsDisabled(wrapper)).toBe(true)
    expect(findButtonByText(wrapper, '保存草稿')).toBeNull()
    expect(findButtonByText(wrapper, '提交')).toBeNull()
  })

  // ---------- matrix: decision axis (canDecide × status) ----------

  it('canDecide=true + status=SUBMITTED → 投标/弃标 buttons shown', () => {
    const wrapper = makeWrapper({
      canFill: false, canDecide: true, evaluation: makeEvaluation('SUBMITTED'),
    })
    expect(findButtonByText(wrapper, '投标')).not.toBeNull()
    expect(findButtonByText(wrapper, '弃标')).not.toBeNull()
  })

  it('canDecide=true + status=DRAFT → NO 投标/弃标 (eval-before-decide)', () => {
    const wrapper = makeWrapper({
      canFill: false, canDecide: true, evaluation: makeEvaluation('DRAFT'),
    })
    expect(findButtonByText(wrapper, '投标')).toBeNull()
    expect(findButtonByText(wrapper, '弃标')).toBeNull()
  })

  it('canDecide=true + status=null → NO 投标/弃标', () => {
    const wrapper = makeWrapper({
      canFill: false, canDecide: true, evaluation: null,
    })
    expect(findButtonByText(wrapper, '投标')).toBeNull()
    expect(findButtonByText(wrapper, '弃标')).toBeNull()
  })

  it('canDecide=false + status=SUBMITTED → NO 投标/弃标', () => {
    const wrapper = makeWrapper({
      canFill: false, canDecide: false, evaluation: makeEvaluation('SUBMITTED'),
    })
    expect(findButtonByText(wrapper, '投标')).toBeNull()
    expect(findButtonByText(wrapper, '弃标')).toBeNull()
  })

  it('canFill=true + canDecide=true + status=SUBMITTED → read-only AND 投标/弃标 shown (assignee == assigner edge case)', () => {
    const wrapper = makeWrapper({
      canFill: true, canDecide: true, evaluation: makeEvaluation('SUBMITTED'),
    })
    expect(formIsDisabled(wrapper)).toBe(true)
    expect(findButtonByText(wrapper, '投标')).not.toBeNull()
    expect(findButtonByText(wrapper, '弃标')).not.toBeNull()
  })

  it('canFill=false + canDecide=false + any status → read-only with NO buttons (read-only viewer)', () => {
    const wrapper = makeWrapper({
      canFill: false, canDecide: false, evaluation: makeEvaluation('SUBMITTED'),
    })
    expect(formIsDisabled(wrapper)).toBe(true)
    expect(findButtonByText(wrapper, '保存草稿')).toBeNull()
    expect(findButtonByText(wrapper, '提交')).toBeNull()
    expect(findButtonByText(wrapper, '投标')).toBeNull()
    expect(findButtonByText(wrapper, '弃标')).toBeNull()
  })

  // ---------- emits / behaviour ----------

  it('submit event fires with full payload when canFill=true and all required fields filled', async () => {
    const wrapper = makeWrapper({ canFill: true, canDecide: false, evaluation: null })
    const payload = makeFullPayload()
    await fillRequiredFields(wrapper, payload)
    await findButtonByText(wrapper, '提交').trigger('click')
    await flushPromises()

    const emitted = wrapper.emitted('submit')
    expect(emitted).toBeTruthy()
    expect(emitted.length).toBe(1)
    expect(emitted[0][0]).toMatchObject({
      projectBackground: payload.projectBackground,
      competitorAnalysis: payload.competitorAnalysis,
      contractPeriodStart: payload.contractPeriodStart,
      contractPeriodEnd: payload.contractPeriodEnd,
      shortlistedCount: payload.shortlistedCount,
      platformServiceFee: payload.platformServiceFee,
    })
  })

  it('submit does NOT fire when required field missing', async () => {
    const wrapper = makeWrapper({ canFill: true, canDecide: false, evaluation: null })
    await fillRequiredFields(wrapper, makeFullPayload({ projectBackground: '' }))
    await findButtonByText(wrapper, '提交').trigger('click')
    await flushPromises()
    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  it('save-draft fires regardless of required-field validation', async () => {
    const wrapper = makeWrapper({ canFill: true, canDecide: false, evaluation: null })
    await fillRequiredFields(wrapper, makeFullPayload({ projectBackground: '' }))
    await findButtonByText(wrapper, '保存草稿').trigger('click')
    await flushPromises()
    expect(wrapper.emitted('save-draft')).toBeTruthy()
    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  it('bid event fires when canDecide=true + SUBMITTED → 投标', async () => {
    const wrapper = makeWrapper({
      canFill: false, canDecide: true, evaluation: makeEvaluation('SUBMITTED'),
    })
    await findButtonByText(wrapper, '投标').trigger('click')
    await flushPromises()
    const emitted = wrapper.emitted('bid')
    expect(emitted).toBeTruthy()
    expect(emitted.length).toBe(1)
  })

  it('abandon fires with { reason } when canDecide=true + SUBMITTED + confirm dialog', async () => {
    elMessageBox.prompt.mockResolvedValueOnce({ value: '客户预算不足' })
    const wrapper = makeWrapper({
      canFill: false, canDecide: true, evaluation: makeEvaluation('SUBMITTED'),
    })
    await findButtonByText(wrapper, '弃标').trigger('click')
    await flushPromises()
    expect(elMessageBox.prompt).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('abandon')[0][0]).toEqual({ reason: '客户预算不足' })
  })

  it('abandon does NOT emit when dialog cancelled', async () => {
    elMessageBox.prompt.mockRejectedValueOnce(new Error('cancel'))
    const wrapper = makeWrapper({
      canFill: false, canDecide: true, evaluation: makeEvaluation('SUBMITTED'),
    })
    await findButtonByText(wrapper, '弃标').trigger('click')
    await flushPromises()
    expect(wrapper.emitted('abandon')).toBeFalsy()
  })

  it('submit succeeds when bidRecommendation is empty (optional field)', async () => {
    const wrapper = makeWrapper({ canFill: true, canDecide: false, evaluation: null })
    await fillRequiredFields(wrapper, makeFullPayload({ bidRecommendation: '' }))
    await findButtonByText(wrapper, '提交').trigger('click')
    await flushPromises()
    expect(wrapper.emitted('submit')).toBeTruthy()
  })

  it('submit does NOT fire when shortlistedCount = 0 (policy requires >= 1)', async () => {
    const wrapper = makeWrapper({ canFill: true, canDecide: false, evaluation: null })
    await fillRequiredFields(wrapper, makeFullPayload({ shortlistedCount: 0 }))
    await findButtonByText(wrapper, '提交').trigger('click')
    await flushPromises()
    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  it('contractPeriodStart > contractPeriodEnd rejected with clear validation error', async () => {
    const wrapper = makeWrapper({ canFill: true, canDecide: false, evaluation: null })
    await fillRequiredFields(
      wrapper,
      makeFullPayload({ contractPeriodStart: '2027-12-31', contractPeriodEnd: '2026-06-01' })
    )
    await findButtonByText(wrapper, '提交').trigger('click')
    await flushPromises()
    expect(wrapper.emitted('submit')).toBeFalsy()

    const messageCalls = [
      ...elMessage.error.mock.calls,
      ...elMessage.warning.mock.calls,
    ]
    const messageText = messageCalls.flat().join(' ')
    const mentionsPeriod = /合同周期|开始日期|结束日期|contract\s*period/i
    expect(mentionsPeriod.test(messageText) || mentionsPeriod.test(wrapper.text())).toBe(true)
  })
})
