import { computed, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const context = {
  bidAgent: {},
}

vi.mock('@/composables/projectDetail/context.js', () => ({
  useProjectDetailContext: () => context,
}))

import ProjectDetailBidAgentDrawer from './ProjectDetailBidAgentDrawer.vue'

const stubs = {
  'el-drawer': {
    props: ['modelValue'],
    template: '<section v-if="modelValue" data-test="drawer"><slot /><div data-test="footer"><slot name="footer" /></div></section>',
  },
  'el-button': {
    props: ['disabled', 'loading', 'type', 'plain'],
    emits: ['click'],
    template: '<button :disabled="disabled || loading" @click="$emit(\'click\')"><slot /></button>',
  },
  'el-tag': { template: '<span class="tag"><slot /></span>' },
  'el-alert': { props: ['title'], template: '<div class="alert">{{ title }}</div>' },
  'el-empty': { props: ['description'], template: '<div class="empty">{{ description }}</div>' },
}

function resetContext(overrides = {}) {
  context.bidAgent = {
    drawerVisible: ref(true),
    currentRun: ref(null),
    applyResult: ref(null),
    error: ref(''),
    creating: ref(false),
    fetching: ref(false),
    applying: ref(false),
    reviewing: ref(false),
    currentRunId: computed(() => context.bidAgent.currentRun.value?.runId || null),
    createRun: vi.fn(),
    fetchRun: vi.fn(),
    applyBidAgentResult: vi.fn(),
    createReview: vi.fn(),
    goToEditor: vi.fn(),
    ...overrides,
  }
}

function mountDrawer() {
  return mount(ProjectDetailBidAgentDrawer, {
    global: {
      stubs: {
        ElDrawer: stubs['el-drawer'],
        ElButton: stubs['el-button'],
        ElTag: stubs['el-tag'],
        ElAlert: stubs['el-alert'],
        ElEmpty: stubs['el-empty'],
      },
    },
  })
}

function buttonByText(wrapper, text) {
  return wrapper.findAll('button, el-button').find((button) => button.text().includes(text))
}

describe('ProjectDetailBidAgentDrawer', () => {
  beforeEach(() => {
    resetContext()
  })

  it('shows the empty state and starts a bid-agent run', async () => {
    const wrapper = mountDrawer()

    expect(wrapper.find('el-empty').attributes('description')).toBe('尚未启动 AI 初稿生成')
    await buttonByText(wrapper, 'AI 生成初稿').trigger('click')

    expect(context.bidAgent.createRun).toHaveBeenCalled()
  })

  it('renders stages, draft sections, sources, confidence, and warnings', () => {
    resetContext({
      currentRun: ref({
        runId: 'run-1',
        status: 'COMPLETED',
        stages: [{ key: 'draft', title: '生成章节', status: 'COMPLETED', message: '已生成' }],
        draft: {
          sections: [{ id: 's1', title: '项目理解', content: '围绕客户需求生成初稿', source: '招标文件', confidence: 92 }],
        },
        risks: [{ title: '需确认交付范围' }],
        gaps: ['缺少报价明细'],
        manualConfirmations: [{ message: '请人工确认资质有效期' }],
      }),
    })

    const wrapper = mountDrawer()

    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.text()).toContain('生成章节')
    expect(wrapper.text()).toContain('项目理解')
    expect(wrapper.text()).toContain('招标文件')
    expect(wrapper.text()).toContain('92%')
    const alertTitles = wrapper.findAll('el-alert').map((alert) => alert.attributes('title'))
    expect(alertTitles).toContain('需确认交付范围')
    expect(alertTitles).toContain('缺少报价明细')
    expect(alertTitles).toContain('请人工确认资质有效期')
  })

  it('can apply the result, request review, and open the editor after apply', async () => {
    resetContext({
      currentRun: ref({ runId: 'run-2', status: 'COMPLETED', draft: { sections: [] } }),
      applyResult: ref({ documentId: 55 }),
    })
    const wrapper = mountDrawer()

    await buttonByText(wrapper, '发起审查').trigger('click')
    await buttonByText(wrapper, '写入文档编辑器').trigger('click')
    await buttonByText(wrapper, '打开文档编辑器').trigger('click')

    expect(context.bidAgent.createReview).toHaveBeenCalled()
    expect(context.bidAgent.applyBidAgentResult).toHaveBeenCalled()
    expect(context.bidAgent.goToEditor).toHaveBeenCalled()
  })
})
