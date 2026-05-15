import { computed, reactive, watch } from 'vue'
// element-plus is imported lazily inside handlers to avoid TDZ issues when
// specs use vi.mock('element-plus') with outer-scoped const factories.

// Defaults for a blank evaluation form. Kept as a factory so each instance
// gets its own fresh object (and so resetting is a pure clone).
function makeEmptyForm() {
  return {
    projectBackground: '',
    competitorAnalysis: '',
    contractPeriodStart: '',
    contractPeriodEnd: '',
    shortlistedCount: null,
    platformServiceFee: null,
    previousQuotation: '',
    bidRecommendation: null,
  }
}

// Map a backend evaluation payload (may be null) into the form's local shape.
// Unknown / undefined fields fall back to defaults so v-model always has a
// defined value to bind to.
export function evaluationToForm(evaluation) {
  if (!evaluation) return makeEmptyForm()
  const blank = makeEmptyForm()
  return {
    projectBackground: evaluation.projectBackground ?? blank.projectBackground,
    competitorAnalysis: evaluation.competitorAnalysis ?? blank.competitorAnalysis,
    contractPeriodStart: evaluation.contractPeriodStart ?? blank.contractPeriodStart,
    contractPeriodEnd: evaluation.contractPeriodEnd ?? blank.contractPeriodEnd,
    shortlistedCount: evaluation.shortlistedCount ?? blank.shortlistedCount,
    platformServiceFee: evaluation.platformServiceFee ?? blank.platformServiceFee,
    previousQuotation: evaluation.previousQuotation ?? blank.previousQuotation,
    bidRecommendation: evaluation.bidRecommendation ?? blank.bidRecommendation,
  }
}

// Pure validation helpers — easy to unit test in isolation.
export function validateRequired(form) {
  const requiredText = ['projectBackground', 'competitorAnalysis']
  for (const key of requiredText) {
    if (!form[key] || !String(form[key]).trim()) return false
  }
  if (!form.contractPeriodStart || !form.contractPeriodEnd) return false
  // C5: shortlistedCount must be present AND >= 1 — backend policy rejects 0.
  if (
    form.shortlistedCount === null ||
    form.shortlistedCount === undefined ||
    Number(form.shortlistedCount) < 1
  ) {
    return false
  }
  if (form.platformServiceFee === null || form.platformServiceFee === undefined) return false
  // C4: bidRecommendation is optional per backend policy — not required client-side.
  return true
}

export function validateContractPeriod(form) {
  if (!form.contractPeriodStart || !form.contractPeriodEnd) return true
  return form.contractPeriodStart <= form.contractPeriodEnd
}

export function useTenderEvaluationForm(props, emit) {
  const form = reactive(makeEmptyForm())

  // Sync incoming evaluation prop -> form. `immediate` covers the initial mount.
  watch(
    () => props.evaluation,
    (next) => {
      const mapped = evaluationToForm(next)
      Object.assign(form, mapped)
    },
    { immediate: true, deep: true }
  )

  // ---- visibility (instance-level, no role enum) --------------------------
  // canFill   → user is latest assignee of this tender (PM-of-this-tender)
  // canDecide → user is latest assigned-by  (bid/abandon decision maker)
  //
  // Decision rule:
  //   editable        := canFill && status !== SUBMITTED
  //   draft/submit    := same as editable (only assignee can write)
  //   bid/abandon     := canDecide && status === SUBMITTED (evaluate first)
  const evaluationStatus = computed(() => props.evaluation?.evaluationStatus || null)
  const isSubmitted = computed(() => evaluationStatus.value === 'SUBMITTED')

  // Vue's `type: Boolean` prop already coerces — no need to re-wrap here.
  const isEditable = computed(() => props.canFill && !isSubmitted.value)
  const isReadOnly = computed(() => !isEditable.value)

  const showDraftSubmitButtons = computed(() => props.canFill && !isSubmitted.value)
  const showDecisionButtons = computed(() => props.canDecide && isSubmitted.value)

  // ---- declarative el-form rules -----------------------------------------
  // Kept small and declarative per the Split-First Rule (no hand-rolled
  // if-else trees here — date-range cross-check is done at submit time
  // because it spans two fields).
  // C4: bidRecommendation is OPTIONAL per backend policy (line 28 of the
  // TenderEvaluationFormPolicy contract). Do not mark it required here.
  const rules = {
    projectBackground: [{ required: true, message: '请填写项目背景', trigger: 'blur' }],
    competitorAnalysis: [{ required: true, message: '请填写竞争对手情况', trigger: 'blur' }],
    contractPeriodStart: [{ required: true, message: '请选择合同开始日期', trigger: 'change' }],
    contractPeriodEnd: [{ required: true, message: '请选择合同结束日期', trigger: 'change' }],
    shortlistedCount: [{ required: true, message: '请填写入围家数', trigger: 'change' }],
    platformServiceFee: [{ required: true, message: '请填写平台服务费', trigger: 'change' }],
  }

  // ---- handlers -----------------------------------------------------------
  function buildPayload() {
    return {
      projectBackground: form.projectBackground,
      competitorAnalysis: form.competitorAnalysis,
      contractPeriodStart: form.contractPeriodStart,
      contractPeriodEnd: form.contractPeriodEnd,
      shortlistedCount: form.shortlistedCount,
      platformServiceFee: form.platformServiceFee,
      previousQuotation: form.previousQuotation,
      bidRecommendation: form.bidRecommendation,
    }
  }

  async function handleSubmit() {
    const { ElMessage } = await import('element-plus')
    if (!validateRequired(form)) {
      ElMessage.warning('请完整填写所有必填字段')
      return
    }
    if (!validateContractPeriod(form)) {
      ElMessage.error('合同周期开始日期不能晚于结束日期')
      return
    }
    emit('submit', buildPayload())
  }

  function handleSaveDraft() {
    emit('save-draft', buildPayload())
  }

  function handleBid() {
    emit('bid')
  }

  async function handleAbandon() {
    const { ElMessage, ElMessageBox } = await import('element-plus')
    try {
      const { value: reason } = await ElMessageBox.prompt(
        '请填写弃标原因（必填）',
        '弃标确认',
        {
          confirmButtonText: '确认弃标',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: '请输入弃标原因...',
          inputErrorMessage: '弃标原因不能为空',
          inputValidator: (v) => Boolean(v && v.trim()) || '弃标原因不能为空',
        }
      )
      const trimmed = (reason || '').trim()
      if (!trimmed) {
        ElMessage.warning('弃标原因不能为空')
        return
      }
      emit('abandon', { reason: trimmed })
    } catch {
      // user cancelled the dialog — no emit
    }
  }

  return {
    form,
    rules,
    isReadOnly,
    isEditable,
    showDraftSubmitButtons,
    showDecisionButtons,
    handleSubmit,
    handleSaveDraft,
    handleBid,
    handleAbandon,
  }
}
