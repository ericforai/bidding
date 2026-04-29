import { test, expect } from '@playwright/test'
import { authedJson, createAuthenticatedSession, createProjectFixture } from './support/project-fixtures.js'

test('project detail workflow persists tasks and documents through real API', async ({ page }) => {
  const session = await createAuthenticatedSession()
  const project = await createProjectFixture(session)

  await page.addInitScript(({ token, user }) => {
    sessionStorage.setItem('token', token)
    sessionStorage.setItem('user', JSON.stringify(user))
  }, session)

  const projectId = String(project.id)
  const projectName = project.name

  await page.goto(`/project/${projectId}`)
  await expect(page).toHaveURL(/\/project\/\d+$/)

  if (projectName) {
    await expect(page.getByText(projectName).first()).toBeVisible()
  }

  await page.getByRole('button', { name: '添加任务' }).click()
  await expect(page.getByText('任务已新增')).toBeVisible()
  await expect(page.getByText('新增任务 1').first()).toBeVisible()

  await page.getByRole('button', { name: '添加文档' }).click()
  await expect(page.getByText('项目文档已新增')).toBeVisible()
  const createdDocumentName = `项目文档_${new Date().toLocaleDateString('zh-CN').replaceAll('/', '')}.docx`
  await expect(page.getByText(createdDocumentName).first()).toBeVisible()

  await page.getByRole('button', { name: '设置提醒' }).click()
  await expect(page.getByText('项目提醒已创建')).toBeVisible()

  const taskPayload = await authedJson(`/api/projects/${projectId}/tasks`, session.token)
  expect(taskPayload?.success).toBeTruthy()
  expect(Array.isArray(taskPayload?.data)).toBeTruthy()
  expect(taskPayload.data.some((task) => task.name === '新增任务 1')).toBeTruthy()

  const documentPayload = await authedJson(`/api/projects/${projectId}/documents`, session.token)
  expect(documentPayload?.success).toBeTruthy()
  expect(Array.isArray(documentPayload?.data)).toBeTruthy()
  expect(documentPayload.data.some((document) => document.name === createdDocumentName)).toBeTruthy()

  await page.reload()
  await expect(page.getByText('新增任务 1').first()).toBeVisible()
  await expect(page.getByText(createdDocumentName).first()).toBeVisible()
})
