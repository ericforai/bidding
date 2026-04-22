import { computed, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

const context = {
  bidAgent: {},
}

vi.mock('@/composables/projectDetail/context.js', () => ({
  useProjectDetailContext: () => context,
}))

import ProjectDetailBidAgentDrawer from './ProjectDetailBidAgentDrawer.vue'

const stubs = {
  ElDrawer: {
    props: ['modelValue'],
    template: '<section v-if="modelValue" data-test="drawer"><slot /></section>',
  },
  ElButton: {
    props: ['disabled', 'loading', 'type', 'plain', 'tag', 'href'],
    emits: ['click'],
    template: '<component :is="tag || \'button\'" :href="href" :disabled="disabled || loading" @click="$emit(\'click\', $event)"><slot /></component>',
  },
  ElTag: { template: '<span><slot /></span>' },
  ElAlert: { props: ['title'], template: '<div>{{ title }}</div>' },
  ElEmpty: { props: ['description'], template: '<div>{{ description }}</div>' },
}

function resetContext() {
  const applyResult = { projectId: 12, documentId: 55, jobId: 'job-3' }
  context.bidAgent = {
    drawerVisible: ref(true),
    currentRun: ref({
      runId: 'run-42',
      status: 'DRAFTED',
      draft: { sections: [{ id: 's1', title: '项目概况', content: '草稿内容' }] },
    }),
    applyResult: ref(applyResult),
    error: ref(''),
    creating: ref(false),
    fetching: ref(false),
    applying: ref(false),
    reviewing: ref(false),
    projectId: ref(12),
    currentRunId: computed(() => context.bidAgent.currentRun.value?.runId || null),
    createRun: vi.fn(),
    fetchRun: vi.fn(),
    applyBidAgentResult: vi.fn(),
    createReview: vi.fn(),
    goToEditor: vi.fn(() => Promise.resolve()),
  }
  return applyResult
}

function mountDrawer() {
  return mount(ProjectDetailBidAgentDrawer, {
    global: { stubs },
  })
}

describe('ProjectDetailBidAgentDrawer editor navigation regression', () => {
  // Regression: ISSUE-002 — the drawer showed a written result but the editor button left users on the project page.
  // Found by /qa on 2026-04-22.
  // Report: .gstack/qa-reports/qa-report-127-0-0-1-2026-04-22.md
  it('renders a real editor href and still invokes router navigation', async () => {
    const applyResult = resetContext()
    const wrapper = mountDrawer()
    const editorLink = wrapper.findAll('el-button, a').find((link) => link.text().includes('打开文档编辑器'))

    expect(editorLink.attributes('tag')).toBe('a')
    expect(editorLink.attributes('href')).toBe('/document/editor/12?bidAgentRunId=run-42&documentId=55&jobId=job-3')

    await editorLink.trigger('click')

    expect(context.bidAgent.goToEditor).toHaveBeenCalledWith(applyResult)
  })
})
