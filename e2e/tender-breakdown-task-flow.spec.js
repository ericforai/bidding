import { test, expect } from '@playwright/test'
import { Buffer } from 'node:buffer'
import { apiBaseUrl, authedJson, createAuthenticatedSession, createProjectFixture } from './support/project-fixtures.js'

function buildPdfFixture(text) {
  const escapePdfText = (value) => value
    .replace(/\\/g, '\\\\')
    .replace(/\(/g, '\\(')
    .replace(/\)/g, '\\)')
  const lines = text.split('\n')
  const content = [
    'BT',
    '/F1 12 Tf',
    '50 760 Td',
    ...lines.flatMap((line, index) => (index === 0
      ? [`(${escapePdfText(line)}) Tj`]
      : ['0 -16 Td', `(${escapePdfText(line)}) Tj`])),
    'ET',
  ].join('\n')
  const objects = [
    '1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n',
    '2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n',
    '3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>\nendobj\n',
    `4 0 obj\n<< /Length ${Buffer.byteLength(content)} >>\nstream\n${content}\nendstream\nendobj\n`,
    '5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n',
  ]
  let output = '%PDF-1.4\n'
  const offsets = [0]
  objects.forEach((object) => {
    offsets.push(Buffer.byteLength(output))
    output += object
  })
  const xrefOffset = Buffer.byteLength(output)
  output += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`
  for (let index = 1; index < offsets.length; index += 1) {
    output += `${String(offsets[index]).padStart(10, '0')} 00000 n \n`
  }
  output += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF\n`
  return Buffer.from(output)
}

async function uploadTenderBreakdownFixture(session, projectId) {
  const form = new FormData()
  form.set(
    'file',
    new Blob([buildPdfFixture([
      'Tender requirements for Xiyu MRO platform.',
      'Business terms response and quotation are required.',
      'Technical implementation plan and integration plan are required.',
      'Qualification documents and project experience proof are required.',
    ].join('\n'))], {
      type: 'application/pdf',
    }),
    'e2e-tender-breakdown.pdf',
  )

  const response = await fetch(`${apiBaseUrl}/api/projects/${projectId}/tender-breakdown`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${session.token}`,
    },
    body: form,
  })
  const payload = await response.json().catch(() => null)
  if (!response.ok || !payload?.success || !payload?.data?.document?.snapshotId) {
    throw new Error(`Unable to upload tender breakdown fixture: ${response.status} ${JSON.stringify(payload)}`)
  }
  return payload.data.document.snapshotId
}

test('tender document breakdown can generate project tasks through real API', async ({ page }) => {
  const session = await createAuthenticatedSession()
  const project = await createProjectFixture(session, '招标文件拆解任务')
  const projectId = String(project.id)
  const snapshotId = await uploadTenderBreakdownFixture(session, projectId)

  await page.addInitScript(({ token, user }) => {
    sessionStorage.setItem('token', token)
    sessionStorage.setItem('user', JSON.stringify(user))
  }, session)

  await page.goto(`/project/${projectId}`)
  await expect(page).toHaveURL(/\/project\/\d+$/)

  await page.locator('[data-test="tender-breakdown-button"]').click()
  const tenderDialog = page.locator('.el-dialog').filter({ hasText: '解析招标文件' })
  await expect(page.getByText(/已复用已解析的招标文件/)).toBeVisible()
  await expect(tenderDialog).toHaveCount(0)

  const latestPayload = await authedJson(`/api/projects/${projectId}/tender-breakdown/latest`, session.token)
  expect(latestPayload?.success).toBeTruthy()
  expect(latestPayload?.data?.document?.snapshotId).toBe(snapshotId)

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
