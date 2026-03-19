import { test, expect } from '@playwright/test'
import { apiBaseUrl, ensureApiSession, injectSession } from './auth-helpers.js'

async function apiRequest(path, session, options = {}) {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${session.token}`,
      ...(options.headers || {}),
    },
  })

  if (!response.ok) {
    throw new Error(`API request failed: ${path} -> ${response.status}`)
  }

  return response.json()
}

test('document editor loads backend sections and persists saved content', async ({ page }) => {
  const session = await ensureApiSession({
    username: `document_editor_${Date.now()}`,
    role: 'ADMIN',
    fullName: 'Document Editor Admin'
  })
  const projectId = 960100 + Date.now() % 10000

  const structurePayload = await apiRequest(`/api/documents/${projectId}/editor/structure`, session, {
    method: 'POST',
    body: JSON.stringify({
      projectId,
      name: `ERI-96 文档结构 ${projectId}`,
    }),
  })

  const structureId = structurePayload?.data?.id
  expect(structureId).toBeTruthy()

  const sectionPayload = await apiRequest(`/api/documents/${projectId}/editor/sections`, session, {
    method: 'POST',
    body: JSON.stringify({
      structureId,
      sectionType: 'SECTION',
      title: '技术说明',
      content: '初始内容',
      orderIndex: 1,
    }),
  })

  const sectionId = sectionPayload?.data?.id
  expect(sectionId).toBeTruthy()

  await injectSession(page, session)

  await page.goto(`/document/editor/${projectId}`)
  await expect(page.getByText('章节目录')).toBeVisible()
  await expect(page.locator('.section-tree-card').getByText('技术说明', { exact: true })).toBeVisible()
  await expect(page.locator('.editor-card').getByText('技术说明', { exact: true })).toBeVisible()

  const textarea = page.locator('textarea.content-textarea')
  await expect(textarea).toHaveValue('初始内容')

  const updatedContent = `更新内容 ${projectId}`
  await textarea.fill(updatedContent)
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText('保存成功')).toBeVisible()

  await page.reload()
  await expect(page.locator('.editor-card').getByText('技术说明', { exact: true })).toBeVisible()
  await expect(page.locator('textarea.content-textarea')).toHaveValue(updatedContent)
})
