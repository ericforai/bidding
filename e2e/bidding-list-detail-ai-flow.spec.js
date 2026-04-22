import { test, expect } from '@playwright/test'
import { apiBaseUrl, ensureApiSession, injectSession } from './auth-helpers.js'

function toLocalDateTimeString(date) {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60 * 1000)
    .toISOString()
    .slice(0, 19)
}

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

async function seedBiddingTender(session, suffix) {
  const tenderPayload = await apiRequest('/api/tenders', session, {
    method: 'POST',
    body: JSON.stringify({
      title: `E2E 标讯主链路 ${suffix}`,
      source: 'Playwright',
      budget: 560000,
      deadline: toLocalDateTimeString(new Date(Date.now() + 10 * 24 * 60 * 60 * 1000)),
      status: 'TRACKING',
      aiScore: 91,
      riskLevel: 'LOW',
      region: '华东',
      industry: '政府',
    }),
  })

  const tender = tenderPayload?.data
  expect(tender?.id).toBeTruthy()
  return tender
}

test.describe('bidding list detail ai flow', () => {
  test('navigates list to detail then ai analysis', async ({ page }) => {
    const suffix = Date.now()
    const session = await ensureApiSession({
      username: `bidding_flow_${suffix}`,
      role: 'ADMIN',
      fullName: 'Bidding Flow Admin',
    })
    const tender = await seedBiddingTender(session, suffix)

    await injectSession(page, session)
    await page.goto('/bidding')

    await expect(page).toHaveURL(/\/bidding$/)
    const row = page.locator('.el-table__row', { hasText: tender.title }).first()
    await expect(row).toBeVisible()

    await row.getByRole('button', { name: '查看详情' }).click()
    await expect(page).toHaveURL(new RegExp(`/bidding/${tender.id}$`))
    await expect(page.getByText(tender.title).first()).toBeVisible()

    await page.getByRole('link', { name: '标讯中心' }).click()
    await expect(page).toHaveURL(/\/bidding$/)

    const refreshedRow = page.locator('.el-table__row', { hasText: tender.title }).first()
    await expect(refreshedRow).toBeVisible()
    await refreshedRow.getByRole('button', { name: 'AI分析' }).click()

    await expect(page).toHaveURL(new RegExp(`/bidding/ai-analysis/${tender.id}$`))
    await expect(page.getByText('AI分析报告')).toBeVisible()
  })

  test('shows clear error feedback when ai analysis detail is unavailable', async ({ page }) => {
    const suffix = Date.now()
    const session = await ensureApiSession({
      username: `bidding_flow_fail_${suffix}`,
      role: 'ADMIN',
      fullName: 'Bidding Flow Fail Admin',
    })

    await injectSession(page, session)
    await page.goto('/bidding/ai-analysis/non-existent-tender-id')

    await expect(page).toHaveURL(/\/bidding\/ai-analysis\/non-existent-tender-id$/)
    await expect(page.getByText('当前模式下暂无可用的 AI 分析报告')).toBeVisible()
    await expect(page.locator('.el-message--error').filter({ hasText: '加载失败' }).first()).toBeVisible()
  })
})
