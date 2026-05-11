// TDD Phase 2: failing-tests-first spec for TenderEvaluationForm.vue
// The component does not yet exist — these tests MUST fail until Phase 3
// implements `./TenderEvaluationForm.vue` to satisfy this contract.

import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

// ---- element-plus mocks ----------------------------------------------------
//
// MessageBox.prompt is used by the abandon-confirm dialog. We mock it at the
// module level so each test can drive resolve/reject without rendering the
// real overlay.
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

// ---- import the component under test --------------------------------------
//
// This import resolves to the file `./TenderEvaluationForm.vue`, which does
// not yet exist. That means Vitest will fail to load this spec until Phase 3
// creates the component — which is exactly what we want for RED phase.
import TenderEvaluationForm from './TenderEvaluationForm.vue'

// ---- shared Element Plus stubs --------------------------------------------
//
// Keep stubs lightweight but functional: they must preserve `label` text so
// `wrapper.text()` can assert on Chinese labels, AND they must forward v-model
// updates so the form's internal reactive state moves the way a real user
// would drive it.
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

// ---- helpers --------------------------------------------------------------

function makeWrapper(props = {}, extra = {}) {
  return mount(TenderEvaluationForm, {
    props: {
      tenderId: 9001,
      evaluation: null,
      currentUserRole: 'MANAGER',
      ...props,
    },
    global: {
      stubs: globalStubs,
      ...(extra.global || {}),
    },
  })
}

// Find a button by its visible Chinese label. Returns the *first* match. If
// not found, returns null so tests can assert non-existence cleanly.
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

// Fill every required field on the form by setting v-model on the appropriate
// stubs. Order matches the field table in the contract spec.
async function fillRequiredFields(wrapper, payload = makeFullPayload()) {
  const textareas = wrapper.findAll('textarea.el-input-stub')
  // textareas: [projectBackground, competitorAnalysis, previousQuotation]
  await textareas[0].setValue(payload.projectBackground)
  await textareas[1].setValue(payload.competitorAnalysis)
  if (textareas[2] && payload.previousQuotation !== undefined) {
    await textareas[2].setValue(payload.previousQuotation)
  }

  const dates = wrapper.findAll('input.el-date-picker-stub')
  // dates: [contractPeriodStart, contractPeriodEnd]
  await dates[0].setValue(payload.contractPeriodStart)
  await dates[1].setValue(payload.contractPeriodEnd)

  const numbers = wrapper.findAll('input.el-input-number-stub')
  // numbers: [shortlistedCount, platformServiceFee]
  await numbers[0].setValue(String(payload.shortlistedCount))
  await numbers[1].setValue(String(payload.platformServiceFee))

  const selects = wrapper.findAll('select.el-select-stub')
  // selects: [bidRecommendation]
  if (selects[0] && payload.bidRecommendation !== undefined) {
    await selects[0].setValue(payload.bidRecommendation)
  }

  await flushPromises()
}

// ===========================================================================
// SUITE
// ===========================================================================

describe('TenderEvaluationForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // -------------------------------------------------------------------------
  // 1. Rendering — fields visible to MANAGER on blank form
  // -------------------------------------------------------------------------
  it('renders all 7 fields when role = MANAGER and evaluation = null', () => {
    const wrapper = makeWrapper({
      currentUserRole: 'MANAGER',
      evaluation: null,
    })

    const text = wrapper.text()
    // Seven labelled fields from the contract.
    expect(text).toContain('项目背景')
    expect(text).toContain('竞争对手情况')
    expect(text).toContain('项目合同周期')
    expect(text).toContain('入围家数')
    expect(text).toContain('平台服务费')
    expect(text).toContain('上一次报价情况')
    expect(text).toContain('建议是否投标')

    // Two textareas for required text fields + one for previousQuotation = 3
    expect(wrapper.findAll('textarea.el-input-stub').length).toBeGreaterThanOrEqual(3)
    // Start + end date pickers
    expect(wrapper.findAll('input.el-date-picker-stub').length).toBeGreaterThanOrEqual(2)
    // shortlistedCount + platformServiceFee
    expect(wrapper.findAll('input.el-input-number-stub').length).toBeGreaterThanOrEqual(2)
    // bidRecommendation select
    expect(wrapper.findAll('select.el-select-stub').length).toBeGreaterThanOrEqual(1)
  })

  // -------------------------------------------------------------------------
  // 2. PM does not see 投标/弃标
  // -------------------------------------------------------------------------
  it('does NOT show "投标"/"弃标" buttons when role = MANAGER', () => {
    const wrapper = makeWrapper({
      currentUserRole: 'MANAGER',
      evaluation: null,
    })

    expect(findButtonByText(wrapper, '投标')).toBeNull()
    expect(findButtonByText(wrapper, '弃标')).toBeNull()
  })

  // -------------------------------------------------------------------------
  // 3. PM sees 保存草稿 + 提交
  // -------------------------------------------------------------------------
  it('shows "保存草稿" + "提交" buttons when role = MANAGER and evaluation is null or DRAFT', () => {
    const blank = makeWrapper({
      currentUserRole: 'MANAGER',
      evaluation: null,
    })
    expect(findButtonByText(blank, '保存草稿')).not.toBeNull()
    expect(findButtonByText(blank, '提交')).not.toBeNull()

    const draft = makeWrapper({
      currentUserRole: 'MANAGER',
      evaluation: {
        evaluationStatus: 'DRAFT',
        projectBackground: '草稿背景',
        competitorAnalysis: '',
        contractPeriodStart: '',
        contractPeriodEnd: '',
        shortlistedCount: null,
        platformServiceFee: null,
        previousQuotation: '',
        bidRecommendation: null,
      },
    })
    expect(findButtonByText(draft, '保存草稿')).not.toBeNull()
    expect(findButtonByText(draft, '提交')).not.toBeNull()
  })

  // -------------------------------------------------------------------------
  // 4. ADMIN sees read-only fields when SUBMITTED
  // -------------------------------------------------------------------------
  it('renders form fields read-only when role = ADMIN and evaluationStatus = SUBMITTED', () => {
    const wrapper = makeWrapper({
      currentUserRole: 'ADMIN',
      evaluation: {
        evaluationStatus: 'SUBMITTED',
        ...makeFullPayload(),
      },
    })

    // The form-level disabled prop should be true (the contract: read-only for
    // admin once submitted).
    const form = wrapper.findComponent({ name: 'ElForm' })
    expect(form.exists()).toBe(true)
    expect(form.props('disabled')).toBe(true)
  })

  // -------------------------------------------------------------------------
  // 5. ADMIN sees 投标/弃标 buttons when SUBMITTED
  // -------------------------------------------------------------------------
  it('shows "投标" + "弃标" buttons when role = ADMIN AND evaluationStatus = SUBMITTED', () => {
    const wrapper = makeWrapper({
      currentUserRole: 'ADMIN',
      evaluation: {
        evaluationStatus: 'SUBMITTED',
        ...makeFullPayload(),
      },
    })
    expect(findButtonByText(wrapper, '投标')).not.toBeNull()
    expect(findButtonByText(wrapper, '弃标')).not.toBeNull()
  })

  // -------------------------------------------------------------------------
  // 6. ADMIN does NOT see 投标/弃标 when DRAFT or null — decision-after-evaluation
  // -------------------------------------------------------------------------
  it('does NOT show "投标"/"弃标" buttons when role = ADMIN AND evaluationStatus = DRAFT (or evaluation is null)', () => {
    const draft = makeWrapper({
      currentUserRole: 'ADMIN',
      evaluation: {
        evaluationStatus: 'DRAFT',
        ...makeFullPayload(),
      },
    })
    expect(findButtonByText(draft, '投标')).toBeNull()
    expect(findButtonByText(draft, '弃标')).toBeNull()

    const empty = makeWrapper({
      currentUserRole: 'ADMIN',
      evaluation: null,
    })
    expect(findButtonByText(empty, '投标')).toBeNull()
    expect(findButtonByText(empty, '弃标')).toBeNull()
  })

  // -------------------------------------------------------------------------
  // 7. submit emits full payload when valid
  // -------------------------------------------------------------------------
  it('submit event fires with full payload when all required fields filled and "提交" clicked', async () => {
    const wrapper = makeWrapper({
      currentUserRole: 'MANAGER',
      evaluation: null,
    })

    const payload = makeFullPayload()
    await fillRequiredFields(wrapper, payload)

    const submitBtn = findButtonByText(wrapper, '提交')
    expect(submitBtn).not.toBeNull()
    await submitBtn.trigger('click')
    await flushPromises()

    const emitted = wrapper.emitted('submit')
    expect(emitted).toBeTruthy()
    expect(emitted.length).toBe(1)

    const arg = emitted[0][0]
    expect(arg).toMatchObject({
      projectBackground: payload.projectBackground,
      competitorAnalysis: payload.competitorAnalysis,
      contractPeriodStart: payload.contractPeriodStart,
      contractPeriodEnd: payload.contractPeriodEnd,
      shortlistedCount: payload.shortlistedCount,
      platformServiceFee: payload.platformServiceFee,
      previousQuotation: payload.previousQuotation,
      bidRecommendation: payload.bidRecommendation,
    })
  })

  // -------------------------------------------------------------------------
  // 8. submit does NOT fire when required field missing
  // -------------------------------------------------------------------------
  it('submit event does NOT fire when a required field is missing (e.g., projectBackground empty)', async () => {
    const wrapper = makeWrapper({
      currentUserRole: 'MANAGER',
      evaluation: null,
    })

    // Fill everything EXCEPT projectBackground.
    await fillRequiredFields(wrapper, makeFullPayload({ projectBackground: '' }))

    const submitBtn = findButtonByText(wrapper, '提交')
    expect(submitBtn).not.toBeNull()
    await submitBtn.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  // -------------------------------------------------------------------------
  // 9. save-draft always fires regardless of required validation
  // -------------------------------------------------------------------------
  it('save-draft event fires regardless of required-field validation', async () => {
    const wrapper = makeWrapper({
      currentUserRole: 'MANAGER',
      evaluation: null,
    })

    // Leave projectBackground empty — save-draft must still go through.
    await fillRequiredFields(wrapper, makeFullPayload({ projectBackground: '' }))

    const draftBtn = findButtonByText(wrapper, '保存草稿')
    expect(draftBtn).not.toBeNull()
    await draftBtn.trigger('click')
    await flushPromises()

    const emitted = wrapper.emitted('save-draft')
    expect(emitted).toBeTruthy()
    expect(emitted.length).toBe(1)

    // Payload shape mirrors submit (no validation enforced).
    const arg = emitted[0][0]
    expect(arg).toMatchObject({
      projectBackground: '',
      competitorAnalysis: '主要竞争对手为 A 公司、B 公司',
      contractPeriodStart: '2026-06-01',
      contractPeriodEnd: '2027-05-31',
      shortlistedCount: 3,
      platformServiceFee: 5000.0,
    })

    // submit must NOT have fired alongside save-draft.
    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  // -------------------------------------------------------------------------
  // 10. bid emits when admin clicks 投标
  // -------------------------------------------------------------------------
  it('bid event fires when admin clicks "投标" (admin role + SUBMITTED status)', async () => {
    const wrapper = makeWrapper({
      currentUserRole: 'ADMIN',
      evaluation: {
        evaluationStatus: 'SUBMITTED',
        ...makeFullPayload(),
      },
    })

    const bidBtn = findButtonByText(wrapper, '投标')
    expect(bidBtn).not.toBeNull()
    await bidBtn.trigger('click')
    await flushPromises()

    const emitted = wrapper.emitted('bid')
    expect(emitted).toBeTruthy()
    expect(emitted.length).toBe(1)
    // 投标 has no payload per the contract.
    expect(emitted[0][0]).toBeUndefined()
  })

  // -------------------------------------------------------------------------
  // 11. abandon emits { reason } after confirmation dialog
  // -------------------------------------------------------------------------
  it('abandon event fires with { reason } when admin clicks "弃标" → fills reason → confirms dialog', async () => {
    elMessageBox.prompt.mockResolvedValueOnce({ value: '客户预算不足' })

    const wrapper = makeWrapper({
      currentUserRole: 'ADMIN',
      evaluation: {
        evaluationStatus: 'SUBMITTED',
        ...makeFullPayload(),
      },
    })

    const abandonBtn = findButtonByText(wrapper, '弃标')
    expect(abandonBtn).not.toBeNull()
    await abandonBtn.trigger('click')
    await flushPromises()

    expect(elMessageBox.prompt).toHaveBeenCalledTimes(1)

    const emitted = wrapper.emitted('abandon')
    expect(emitted).toBeTruthy()
    expect(emitted.length).toBe(1)
    expect(emitted[0][0]).toEqual({ reason: '客户预算不足' })
  })

  // -------------------------------------------------------------------------
  // 12. abandon does NOT emit when admin cancels dialog
  // -------------------------------------------------------------------------
  it('abandon event does NOT fire if admin cancels the reason dialog', async () => {
    elMessageBox.prompt.mockRejectedValueOnce(new Error('cancel'))

    const wrapper = makeWrapper({
      currentUserRole: 'ADMIN',
      evaluation: {
        evaluationStatus: 'SUBMITTED',
        ...makeFullPayload(),
      },
    })

    const abandonBtn = findButtonByText(wrapper, '弃标')
    expect(abandonBtn).not.toBeNull()
    await abandonBtn.trigger('click')
    await flushPromises()

    expect(elMessageBox.prompt).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('abandon')).toBeFalsy()
  })

  // -------------------------------------------------------------------------
  // 12.1 (C4) submit succeeds when bidRecommendation is empty (optional field)
  // -------------------------------------------------------------------------
  it('submit succeeds when bidRecommendation is empty (it is optional per policy)', async () => {
    const wrapper = makeWrapper({
      currentUserRole: 'MANAGER',
      evaluation: null,
    })

    // Fill everything EXCEPT bidRecommendation (leave it as ''/null).
    await fillRequiredFields(wrapper, makeFullPayload({ bidRecommendation: '' }))

    const submitBtn = findButtonByText(wrapper, '提交')
    await submitBtn.trigger('click')
    await flushPromises()

    const emitted = wrapper.emitted('submit')
    expect(emitted).toBeTruthy()
    expect(emitted.length).toBe(1)
  })

  // -------------------------------------------------------------------------
  // 12.2 (C5) shortlistedCount = 0 must be blocked client-side
  // -------------------------------------------------------------------------
  it('submit does NOT fire when shortlistedCount = 0 (policy requires >= 1)', async () => {
    const wrapper = makeWrapper({
      currentUserRole: 'MANAGER',
      evaluation: null,
    })

    await fillRequiredFields(wrapper, makeFullPayload({ shortlistedCount: 0 }))

    const submitBtn = findButtonByText(wrapper, '提交')
    await submitBtn.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  // -------------------------------------------------------------------------
  // 13. contractPeriodStart must not be after contractPeriodEnd
  // -------------------------------------------------------------------------
  it('contractPeriodStart must not be after contractPeriodEnd — submit rejected with clear validation error', async () => {
    const wrapper = makeWrapper({
      currentUserRole: 'MANAGER',
      evaluation: null,
    })

    // Start AFTER end — must fail.
    await fillRequiredFields(
      wrapper,
      makeFullPayload({
        contractPeriodStart: '2027-12-31',
        contractPeriodEnd: '2026-06-01',
      })
    )

    const submitBtn = findButtonByText(wrapper, '提交')
    expect(submitBtn).not.toBeNull()
    await submitBtn.trigger('click')
    await flushPromises()

    // Submit must NOT have fired because start > end is invalid.
    expect(wrapper.emitted('submit')).toBeFalsy()

    // The component must surface a clear error to the user. We accept either
    // an ElMessage.error / ElMessage.warning call OR visible error text in
    // the DOM mentioning the contract period.
    const messageCalls = [
      ...elMessage.error.mock.calls,
      ...elMessage.warning.mock.calls,
    ]
    const messageText = messageCalls.flat().join(' ')
    const surfacedInDom = wrapper.text()

    const mentionsPeriod = /合同周期|开始日期|结束日期|contract\s*period/i
    expect(
      mentionsPeriod.test(messageText) || mentionsPeriod.test(surfacedInDom)
    ).toBe(true)
  })
})
