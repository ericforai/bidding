import { test, expect } from '@playwright/test'

const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'

async function requestJson(url, options = {}) {
  const response = await fetch(url, options)
  const payload = await response.json().catch(() => null)

  if (!response.ok) {
    throw new Error(`${options.method || 'GET'} ${url} failed with status ${response.status}: ${JSON.stringify(payload)}`)
  }

  return payload
}

async function createAuthenticatedSession() {
  const username = process.env.COMMERCIAL_E2E_USERNAME || `eri92_${Date.now()}`
  const password = process.env.COMMERCIAL_E2E_PASSWORD || 'XiyuDemo!2026'
  let payload

  try {
    payload = await requestJson(`${apiBaseUrl}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password }),
    })
  } catch {
    payload = await requestJson(`${apiBaseUrl}/api/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username,
        password,
        email: `${username}@example.com`,
        fullName: 'ERI-92 E2E',
        role: 'ADMIN',
      }),
    })
  }

  if (!payload?.success || !payload?.data?.token || !payload?.data?.id) {
    throw new Error('Backend login response is missing token or user identity')
  }

  return {
    token: payload.data.token,
    user: {
      id: payload.data.id,
      name: payload.data.fullName || payload.data.username,
      username: payload.data.username,
      email: payload.data.email,
      role: String(payload.data.role || '').toLowerCase(),
    },
  }
}

async function authedJson(path, token, options = {}) {
  const headers = {
    Authorization: `Bearer ${token}`,
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
  }

  return requestJson(`${apiBaseUrl}${path}`, {
    ...options,
    headers,
  })
}

function toLocalDateTimeString(date) {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60 * 1000)
    .toISOString()
    .slice(0, 19)
}

async function createProjectFixture(session) {
  const suffix = Date.now()
  const tenderPayload = await authedJson('/api/tenders', session.token, {
    method: 'POST',
    body: JSON.stringify({
      title: `E2E 项目详情流程标讯 ${suffix}`,
      source: 'Playwright',
      budget: 880000,
      deadline: toLocalDateTimeString(new Date(Date.now() + 14 * 24 * 60 * 60 * 1000)),
      status: 'TRACKING',
      aiScore: 86,
      riskLevel: 'LOW',
    }),
  })

  const tenderId = tenderPayload?.data?.id
  if (!tenderId) {
    throw new Error('Unable to create tender fixture for project detail workflow E2E')
  }

  const projectPayload = await authedJson('/api/projects', session.token, {
    method: 'POST',
    body: JSON.stringify({
      name: `E2E 项目详情流程项目 ${suffix}`,
      tenderId,
      status: 'PREPARING',
      managerId: session.user.id,
      teamMembers: [session.user.id],
      startDate: toLocalDateTimeString(new Date()),
      endDate: toLocalDateTimeString(new Date(Date.now() + 10 * 24 * 60 * 60 * 1000)),
    }),
  })

  const project = projectPayload?.data
  if (!project?.id) {
    throw new Error('Unable to create project fixture for project detail workflow E2E')
  }

  return project
}

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
