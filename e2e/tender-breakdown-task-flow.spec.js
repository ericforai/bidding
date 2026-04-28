import { test, expect } from '@playwright/test'
import { Buffer } from 'node:buffer'
import { authedJson, createAuthenticatedSession, createProjectFixture } from './support/project-fixtures.js'

test('tender document breakdown can generate project tasks through real API', async ({ page }) => {
  const session = await createAuthenticatedSession()
  const project = await createProjectFixture(session, '招标文件拆解任务')
  const projectId = String(project.id)

  await page.addInitScript(({ token, user }) => {
    sessionStorage.setItem('token', token)
    sessionStorage.setItem('user', JSON.stringify(user))
  }, session)

  await page.goto(`/project/${projectId}`)
  await expect(page).toHaveURL(/\/project\/\d+$/)

  await page.locator('[data-test="tender-breakdown-button"]').click()
  const tenderDialog = page.locator('.el-dialog').filter({ hasText: '解析招标文件' })
  await expect(tenderDialog).toBeVisible()

  await tenderDialog.locator('input[type="file"]').setInputFiles({
    name: 'e2e-tender-breakdown.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('E2E tender fixture; backend e2e profile provides deterministic extracted text.'),
  })

  await expect(page.getByText('招标文件已拆解，可继续生成任务或标书初稿')).toBeVisible()
  await expect(tenderDialog).toBeHidden()

  await page.getByRole('button', { name: '拆解任务' }).click()
  await expect(page.getByText(/已拆解生成 \d+ 个任务/)).toBeVisible()

  await expect(page.getByText('商务标：商务条款响应')).toBeVisible()
  await expect(page.getByText('技术标：平台实施方案')).toBeVisible()
  await expect(page.getByText('资料收集：企业资质材料')).toBeVisible()

  const taskPayload = await authedJson(`/api/projects/${projectId}/tasks`, session.token)
  expect(taskPayload?.success).toBeTruthy()
  expect(taskPayload?.data?.map((task) => task.name)).toEqual(expect.arrayContaining([
    '商务标：商务条款响应',
    '技术标：平台实施方案',
    '资料收集：企业资质材料',
  ]))
})
