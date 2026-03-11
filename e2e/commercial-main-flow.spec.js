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
})
