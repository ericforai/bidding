import fs from 'node:fs'
import path from 'node:path'
import { test, expect } from '@playwright/test'

function resolveUatReportPath() {
  if (process.env.UAT_REPORT_JSON) {
    return process.env.UAT_REPORT_JSON
  }

  const reportDir = path.resolve(process.cwd(), 'docs/reports')
  const candidates = fs.readdirSync(reportDir)
    .filter((name) => name.startsWith('uat-report-') && name.endsWith('.json'))
    .sort()
    .reverse()

  if (candidates.length === 0) {
    throw new Error('No UAT report JSON found for Playwright gate')
  }

  return path.join(reportDir, candidates[0])
}

function loadArtifacts() {
  const reportPath = resolveUatReportPath()
  const payload = JSON.parse(fs.readFileSync(reportPath, 'utf8'))
  if (!payload?.artifacts?.username || !payload?.artifacts?.password) {
    throw new Error(`UAT report is missing login artifacts: ${reportPath}`)
  }
  return payload.artifacts
}

function resolveLoginCredentials() {
  if (process.env.COMMERCIAL_E2E_USERNAME && process.env.COMMERCIAL_E2E_PASSWORD) {
    return {
      username: process.env.COMMERCIAL_E2E_USERNAME,
      password: process.env.COMMERCIAL_E2E_PASSWORD,
    }
  }

  return {
    username: 'lizong',
    password: 'XiyuDemo!2026',
  }
}

async function requestSession(apiBaseUrl, credentials) {
  const response = await fetch(`${apiBaseUrl}/api/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials),
  })

  if (!response.ok) {
    throw new Error(`Backend login failed with status ${response.status}`)
  }

  const payload = await response.json()
  const auth = payload?.data
  if (!payload?.success || !auth?.token || !auth?.id) {
    throw new Error('Backend login response is missing token or user identity')
  }

  return {
    token: auth.token,
    user: {
      id: auth.id,
      name: auth.fullName || auth.username,
      username: auth.username,
      email: auth.email,
      role: String(auth.role || '').toLowerCase(),
    },
  }
}

async function createAuthenticatedSession(credentials) {
  const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:18080'
  const candidateCredentials = [
    credentials,
    { username: 'lizong', password: 'XiyuDemo!2026' },
  ].filter((item, index, list) => index === list.findIndex(other => other.username === item.username && other.password === item.password))

  let lastError = null
  for (const candidate of candidateCredentials) {
    try {
      return await requestSession(apiBaseUrl, {
        username: String(candidate.username || '').trim(),
        password: String(candidate.password || '').trim(),
      })
    } catch (error) {
      lastError = error
    }
  }

  throw lastError || new Error('Unable to create authenticated session')
}

test.describe('commercial main flow', () => {
  test('commercial scope routes render seeded API data', async ({ page }) => {
    const artifacts = loadArtifacts()

    await page.goto('/login')
    await page.getByPlaceholder('请输入用户名').fill(artifacts.username)
    await page.getByPlaceholder('请输入密码').fill(artifacts.password)
    await page.getByRole('button', { name: '登录' }).click()

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByText('工作台').first()).toBeVisible()

    await page.goto('/project')
    await expect(page.locator('.card-header .title').filter({ hasText: '投标项目列表' })).toBeVisible()
    await expect(page.getByText(artifacts.projectName).first()).toBeVisible()

    await page.goto('/knowledge/case')
    await expect(page.getByRole('heading', { name: '案例库' })).toBeVisible()
    await expect(page.getByText(artifacts.caseTitle).first()).toBeVisible()

    await page.goto('/resource/expense')
    await expect(page.locator('.card-header span').filter({ hasText: '费用台账' }).first()).toBeVisible()
    await expect(page.getByText(artifacts.projectName).first()).toBeVisible()

    await page.goto('/resource/bar/sites')
    await expect(page.getByRole('heading', { name: '站点台账' })).toBeVisible()
    await expect(page.getByText(artifacts.assetName).first()).toBeVisible()

    await page.goto('/analytics/dashboard')
    await expect(page.getByRole('heading', { name: '数据分析' })).toBeVisible()
    await expect(page.getByText('中标率趋势')).toBeVisible()
  })

  test('project detail collaboration dialogs open on real project route', async ({ page }) => {
    const credentials = resolveLoginCredentials()
    const session = await createAuthenticatedSession(credentials)

    await page.addInitScript(({ token, user }) => {
      sessionStorage.setItem('token', token)
      sessionStorage.setItem('user', JSON.stringify(user))
    }, session)
    await page.goto('/project')
    const firstProjectLink = page.locator('a.el-link').first()
    await expect(firstProjectLink).toBeVisible()
    const projectName = (await firstProjectLink.textContent())?.trim()
    await firstProjectLink.click()
    await expect(page).toHaveURL(/\/project\/.+$/)
    if (projectName) {
      await expect(page.getByText(projectName).first()).toBeVisible()
    }

    await page.getByRole('button', { name: '智能助手' }).click()
    await expect(page.getByRole('dialog').getByText('智能助手')).toBeVisible()

    await page.getByText('版本管理').click()
    await expect(page.getByRole('dialog').getByRole('heading', { name: '版本历史' })).toBeVisible()
    await page.getByRole('dialog').getByRole('button', { name: '关闭', exact: true }).click()

    await page.getByText('协作中心').click()
    await expect(page.getByRole('dialog').getByText('章节分配', { exact: true })).toBeVisible()
    await expect(page.getByRole('dialog').getByText('变更记录', { exact: true })).toBeVisible()
  })
})
