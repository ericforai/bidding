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

async function ensureSession() {
  const username = process.env.COMMERCIAL_E2E_USERNAME || `eri101_${Date.now()}`
  const password = process.env.COMMERCIAL_E2E_PASSWORD || 'XiyuDemo!2026'
  const email = `${username}@example.com`

  try {
    const payload = await requestJson(`${apiBaseUrl}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username,
        password,
        email,
        fullName: 'ERI-101 E2E',
        role: 'ADMIN',
      }),
    })
    if (payload?.success && payload?.data?.token && payload?.data?.id) {
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
  } catch (error) {
    if (!String(error.message).includes('409')) throw error
  }

  const payload = await requestJson(`${apiBaseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })

  if (!payload?.success || !payload?.data?.token || !payload?.data?.id) {
    throw new Error('Backend login response missing token or user identity')
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

async function authedJson(path, session, options = {}) {
  const headers = {
    Authorization: `Bearer ${session.token}`,
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
  }
  return requestJson(`${apiBaseUrl}${path}`, { ...options, headers })
}

function toLocalDateTimeString(date) {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60 * 1000)
    .toISOString()
    .slice(0, 19)
}

async function waitForAuditItems(session, minimumCount = 1) {
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const payload = await authedJson('/api/audit', session)
    if (Array.isArray(payload?.data?.items) && payload.data.items.length >= minimumCount) {
      return payload
    }
    await new Promise((resolve) => setTimeout(resolve, 500))
  }

  throw new Error('Audit items were not visible before timeout')
}

test('dashboard and audit screens render real operational data', async ({ page }) => {
  const session = await ensureSession()
  const suffix = Date.now()

  const officeTender = await authedJson('/api/tenders', session, {
    method: 'POST',
    body: JSON.stringify({
      title: `智慧办公平台项目 ${suffix}`,
      source: 'ERI-101',
      budget: 530000,
      deadline: toLocalDateTimeString(new Date(Date.now() + 5 * 24 * 60 * 60 * 1000)),
      status: 'TRACKING',
      aiScore: 84,
      riskLevel: 'LOW',
    }),
  })
  expect(officeTender?.data?.id).toBeTruthy()

  const cloudTender = await authedJson('/api/tenders', session, {
    method: 'POST',
    body: JSON.stringify({
      title: `云服务扩容项目 ${suffix}`,
      source: 'ERI-101',
      budget: 910000,
      deadline: toLocalDateTimeString(new Date(Date.now() + 8 * 24 * 60 * 60 * 1000)),
      status: 'TRACKING',
      aiScore: 79,
      riskLevel: 'MEDIUM',
    }),
  })
  expect(cloudTender?.data?.id).toBeTruthy()

  const projectPayload = await authedJson('/api/projects', session, {
    method: 'POST',
    body: JSON.stringify({
      name: `ERI-101 运营看板项目 ${suffix}`,
      tenderId: officeTender.data.id,
      status: 'BIDDING',
      managerId: session.user.id,
      teamMembers: [session.user.id],
      startDate: toLocalDateTimeString(new Date()),
      endDate: toLocalDateTimeString(new Date(Date.now() + 10 * 24 * 60 * 60 * 1000)),
    }),
  })
  expect(projectPayload?.data?.id).toBeTruthy()

  await authedJson('/api/knowledge/templates', session)

  const auditBefore = await waitForAuditItems(session)
  expect(Array.isArray(auditBefore?.data?.items)).toBeTruthy()
  expect(auditBefore.data.items.length).toBeGreaterThan(0)

  await page.addInitScript(({ token, user }) => {
    sessionStorage.setItem('token', token)
    sessionStorage.setItem('user', JSON.stringify(user))
  }, session)

  await page.goto('/analytics/dashboard')
  await expect(page.getByRole('heading', { name: '数据分析' })).toBeVisible()
  await expect(page.getByText('投入产出分析（按产品线）')).toBeVisible()
  await expect(page.getByText('中标率趋势')).toBeVisible()
  await expect(page.locator('.metric-cards .b2b-metric-card').first()).toBeVisible()

  await page.goto('/settings')
  await expect(page.locator('#main-content').getByText('系统设置')).toBeVisible()
  await page.getByRole('tab', { name: '审计日志' }).click()
  await expect(page.getByText('今日操作')).toBeVisible()
  await expect(page.locator('.audit-table .el-table__row').first()).toBeVisible()
  await expect(page.getByText('export').or(page.getByText('审计日志')).first()).toBeVisible()

  await page.getByPlaceholder('搜索操作内容/对象').fill('project')
  await page.getByRole('button', { name: /搜索/ }).click()
  await expect(page.locator('.audit-table .el-table__row').first()).toBeVisible()
})
