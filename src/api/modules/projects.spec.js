// Input: projects API module with mocked HTTP client
// Output: project task decomposition endpoint coverage
// Pos: src/api/modules/ - API module unit tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}))

import httpClient from '@/api/client'
import { projectsApi } from './projects.js'

describe('projectsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('decomposeTasks(): posts to the real project task decomposition endpoint', async () => {
    const payload = { strategy: 'fromTender' }
    httpClient.post.mockResolvedValue({
      success: true,
      data: [{ id: 1, name: '资格文件整理' }],
    })

    await projectsApi.decomposeTasks(12, payload)

    expect(httpClient.post).toHaveBeenCalledWith('/api/projects/12/tasks/decompose', payload, { silentError: true })
  })

  it('decomposeTasks(): rejects non-numeric project IDs before request', async () => {
    const result = await projectsApi.decomposeTasks('PROJECT_12')

    expect(httpClient.post).not.toHaveBeenCalled()
    expect(result.success).toBe(false)
  })

  it('decomposeTasks(): keeps demo project IDs read-only', async () => {
    const result = await projectsApi.decomposeTasks(-1)

    expect(httpClient.post).not.toHaveBeenCalled()
    expect(result.success).toBe(false)
    expect(result.message).toContain('Demo records are read-only')
  })
})
