import { mount, flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const addCommentMock = vi.fn()
const getThreadMock = vi.fn()
const getThreadsMock = vi.fn()
const mentionsCreateMock = vi.fn()
const messageSuccess = vi.fn()
const messageWarning = vi.fn()
const messageError = vi.fn()

vi.mock('@/stores/project.js', () => ({
  useProjectStore: () => ({ projects: [{ id: 9, name: '样本项目' }] }),
}))

vi.mock('@/api/modules/collaboration.js', () => ({
  collaborationApi: {
    addComment: (...args) => addCommentMock(...args),
    getThread: (...args) => getThreadMock(...args),
    getThreads: (...args) => getThreadsMock(...args),
  },
}))

vi.mock('@/api/modules/mentions.js', () => ({
  mentionsApi: {
    create: (...args) => mentionsCreateMock(...args),
  },
}))

vi.mock('@/components/common/MentionInput.vue', () => ({
  default: {
    name: 'MentionInput',
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<textarea class="mention-input-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  },
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: (...args) => messageSuccess(...args),
      warning: (...args) => messageWarning(...args),
      error: (...args) => messageError(...args),
    },
  }
})

const mountCollaboration = async () => {
  setActivePinia(createPinia())
  getThreadsMock.mockResolvedValue({ data: [{ id: 42, title: '投标评估', status: 'OPEN' }] })
  const Collaboration = (await import('./Collaboration.vue')).default
  const wrapper = mount(Collaboration, {
    global: {
      stubs: {
        'el-select': { template: '<div><slot /></div>' },
        'el-option': { template: '<div />' },
        'el-tabs': { template: '<div><slot /></div>' },
        'el-tab-pane': { template: '<div><slot /></div>' },
        'el-card': {
          template: '<div class="el-card-stub" @click="$emit(\'click\')"><slot /></div>',
          emits: ['click'],
        },
        'el-tag': { template: '<span><slot /></span>' },
        'el-button': {
          template: '<button class="btn-stub" @click="$emit(\'click\')"><slot /></button>',
          emits: ['click'],
        },
      },
    },
  })
  await flushPromises()
  // 选中第一条讨论线程 → selectedThread 赋值 + 进入详情 tab
  await wrapper.find('.el-card-stub').trigger('click')
  await flushPromises()
  return wrapper
}

const setCommentText = async (wrapper, text) => {
  const textarea = wrapper.find('textarea.mention-input-stub')
  await textarea.setValue(text)
}

const clickSend = async (wrapper) => {
  const buttons = wrapper.findAll('button.btn-stub')
  await buttons[buttons.length - 1].trigger('click')
  await flushPromises()
}

describe('Collaboration.vue addComment', () => {
  beforeEach(() => {
    addCommentMock.mockReset()
    getThreadMock.mockReset()
    getThreadsMock.mockReset()
    mentionsCreateMock.mockReset()
    messageSuccess.mockReset()
    messageWarning.mockReset()
    messageError.mockReset()
    getThreadMock.mockResolvedValue({ data: { comments: [] } })
  })

  it('不含 @ 时只调用 addComment，不调用 mentionsApi', async () => {
    addCommentMock.mockResolvedValue({ success: true })
    const wrapper = await mountCollaboration()
    await setCommentText(wrapper, '这个先看下')
    await clickSend(wrapper)

    expect(addCommentMock).toHaveBeenCalledWith(42, { content: '这个先看下' })
    expect(mentionsCreateMock).not.toHaveBeenCalled()
    expect(messageSuccess).toHaveBeenCalledWith('评论已发送')
  })

  it('含 @ 时先写评论（plainText），再调 mentionsApi（原始 raw）', async () => {
    addCommentMock.mockResolvedValue({ success: true })
    mentionsCreateMock.mockResolvedValue({ success: true })
    const wrapper = await mountCollaboration()
    await setCommentText(wrapper, '麻烦 @[张三](7) 看一下')
    await clickSend(wrapper)

    expect(addCommentMock).toHaveBeenCalledWith(42, { content: '麻烦 @张三 看一下' })
    expect(mentionsCreateMock).toHaveBeenCalledWith({
      content: '麻烦 @[张三](7) 看一下',
      sourceEntityType: 'COMMENT',
      sourceEntityId: 42,
      title: '投标评估',
    })
    expect(messageSuccess).toHaveBeenCalledWith('评论已发送')
  })

  it('addComment 失败时不触发 mentionsApi，提示发送失败', async () => {
    addCommentMock.mockRejectedValue(new Error('boom'))
    const wrapper = await mountCollaboration()
    await setCommentText(wrapper, '麻烦 @[张三](7) 看一下')
    await clickSend(wrapper)

    expect(addCommentMock).toHaveBeenCalledTimes(1)
    expect(mentionsCreateMock).not.toHaveBeenCalled()
    expect(messageError).toHaveBeenCalledWith('发送失败')
    expect(messageSuccess).not.toHaveBeenCalled()
  })

  it('addComment 成功但 mentionsApi 失败时，评论保留并提示 @ 通知失败', async () => {
    addCommentMock.mockResolvedValue({ success: true })
    mentionsCreateMock.mockRejectedValue(new Error('mention down'))
    const wrapper = await mountCollaboration()
    await setCommentText(wrapper, '麻烦 @[张三](7) 看一下')
    await clickSend(wrapper)

    expect(addCommentMock).toHaveBeenCalledTimes(1)
    expect(mentionsCreateMock).toHaveBeenCalledTimes(1)
    expect(messageWarning).toHaveBeenCalledWith('评论已发送，但 @ 通知发送失败')
    expect(messageSuccess).not.toHaveBeenCalled()
  })
})
