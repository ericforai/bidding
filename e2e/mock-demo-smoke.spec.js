import { test, expect } from '@playwright/test'

const ROUTE_ASSERTIONS = [
  { path: '/dashboard', readyText: '工作台', dataText: '某央企智慧办公平台' },
  { path: '/bidding', readyText: '标讯中心', dataText: '某央企智慧办公平台采购项目' },
  { path: '/bidding/B001', readyText: '标讯详情', dataText: '某央企智慧办公平台采购项目' },
  { path: '/bidding/ai-analysis/B001', readyText: 'AI分析', dataText: '高度匹配，建议积极参与' },
  { path: '/ai-center', readyText: 'AI 智能中心', dataText: 'AI 分析' },
  { path: '/project', readyText: '投标项目列表', dataText: '某央企智慧办公平台采购' },
  { path: '/project/create', readyText: '创建项目', dataText: '项目详情' },
  { path: '/project/P001', readyText: '智能助手', dataText: '某央企智慧办公平台采购' },
  { path: '/knowledge/qualification', readyText: '资质库', dataText: 'ISO9001质量管理体系认证' },
  { path: '/knowledge/case', readyText: '案例库', dataText: '某省政府OA办公系统' },
  { path: '/knowledge/case/detail?id=C001', readyText: '案例详情', dataText: '某省政府OA办公系统' },
  { path: '/knowledge/template', readyText: '模板库', dataText: '智慧办公平台技术方案模板' },
  { path: '/resource/expense', readyText: '费用台账', dataText: '某央企项目' },
  { path: '/resource/account', readyText: '账户管理', dataText: '政府采购网' },
  { path: '/resource/bid-result', readyText: '投标结果闭环', dataText: '深圳地铁自动化系统' },
  { path: '/resource/bar', readyText: '可投标能力检查', dataText: '中国政府采购网' },
  { path: '/resource/bar/sites', readyText: '站点台账', dataText: '中国政府采购网' },
  { path: '/resource/bar/site/S001', readyText: '返回列表', dataText: '中国政府采购网' },
  { path: '/resource/bar/sop/S001', readyText: '找回SOP', dataText: '中国政府采购网' },
  { path: '/analytics/dashboard', readyText: '数据分析', dataText: '中标率趋势' },
  { path: '/settings', readyText: '系统设置', dataText: '用户管理' },
  { path: '/document/editor/P001', readyText: '章节目录', dataText: '智慧城市标书模板' },
]

async function loginAsAdmin(page) {
  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill('李总')
  await page.getByPlaceholder('请输入密码').fill('demo123')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
}

async function expectNoRuntimeErrors(page, errors) {
  await page.waitForTimeout(200)
  expect(errors, errors.map((item) => item.message).join('\n')).toHaveLength(0)
}

test.describe('mock demo smoke', () => {
  test.beforeEach(async ({ page }) => {
    page.on('dialog', async (dialog) => {
      await dialog.accept()
    })
  })

  test('all demo routes render mock data without runtime errors', async ({ page }) => {
    const errors = []
    page.on('pageerror', (error) => errors.push(error))

    await loginAsAdmin(page)

    for (const route of ROUTE_ASSERTIONS) {
      errors.length = 0
      await page.goto(route.path)
      await expect(page.getByText(route.readyText, { exact: false }).first()).toBeVisible()
      await expect(page.getByText(route.dataText, { exact: false }).first()).toBeVisible()
      await expectNoRuntimeErrors(page, errors)
    }
  })

  test('key demo interactions are actually clickable in mock mode', async ({ page }) => {
    const errors = []
    page.on('pageerror', (error) => errors.push(error))

    await loginAsAdmin(page)

    await page.goto('/dashboard')
    await page.locator('.metric-card').nth(0).click()
    await expect(page).toHaveURL(/\/analytics\/dashboard\?drilldown=revenue$/)
    await expect(page.getByText('中标金额明细', { exact: true })).toBeVisible()
    await page.goto('/dashboard')
    await page.locator('.metric-card').nth(1).click()
    await expect(page).toHaveURL(/\/analytics\/dashboard\?drilldown=win-rate$/)
    await expect(page.getByText('中标率明细', { exact: true })).toBeVisible()
    await page.goto('/dashboard')
    await page.locator('.metric-card').nth(2).click()
    await expect(page).toHaveURL(/\/analytics\/dashboard\?drilldown=team$/)
    await expect(page.getByText('团队参与明细', { exact: true })).toBeVisible()
    await page.goto('/dashboard')
    await page.locator('.metric-card').nth(3).click()
    await expect(page).toHaveURL(/\/analytics\/dashboard\?drilldown=projects&status=in_progress$/)
    await expect(page.getByText('进行中项目明细', { exact: true })).toBeVisible()
    await page.goto('/dashboard')
    await page.getByRole('button', { name: '业绩报表' }).click()
    await expect(page).toHaveURL(/\/analytics\/dashboard$/)

    await page.goto('/bidding')
    await page.getByRole('button', { name: '一键获取标讯' }).click()
    const fetchDialog = page.getByRole('dialog')
    await expect(fetchDialog.getByText('标讯获取结果')).toBeVisible()
    await fetchDialog.getByRole('button', { name: '关闭', exact: true }).click()
    await page.getByRole('button', { name: '市场洞察' }).click()
    await expect(page.getByRole('dialog').getByText('市场洞察与趋势预测')).toBeVisible()
    await page.keyboard.press('Escape')

    await page.goto('/bidding/B001')
    await page.getByRole('button', { name: '立即投标' }).click()
    await expect(page).toHaveURL(/\/project\/create(\?.*)?$/)
    await page.goto('/bidding/B001')
    await page.getByRole('button', { name: '加入关注' }).click()
    await expect(page.getByRole('button', { name: '已关注' })).toBeVisible()

    await page.goto('/project/P001')
    await page.getByRole('button', { name: '智能助手' }).click()
    await expect(page.getByRole('dialog').getByText('智能助手')).toBeVisible()
    await page.getByText('版本管理').click()
    await expect(page.getByRole('heading', { name: '版本历史' })).toBeVisible()
    await page.getByRole('dialog').getByRole('button', { name: '关闭', exact: true }).click()
    await page.getByText('协作中心').click()
    await expect(page.getByRole('dialog').getByText('章节分配', { exact: true })).toBeVisible()
    await page.keyboard.press('Escape')
    await page.keyboard.press('Escape')
    await page.getByRole('button', { name: '添加任务' }).click()
    await expect(page.getByText('新增任务 6', { exact: true })).toBeVisible()
    await page.getByRole('button', { name: '添加文档' }).click()
    const mockDocumentName = `项目文档_${new Date().toLocaleDateString('zh-CN').replaceAll('/', '')}.docx`
    await expect(page.getByText(mockDocumentName, { exact: true })).toBeVisible()
    await page.getByRole('button', { name: '设置提醒' }).click()
    await expect(page.getByText('设置了项目跟进提醒')).toBeVisible()

    await page.goto('/knowledge/template')
    await page.getByRole('button', { name: '预览' }).first().click()
    await expect(page.getByRole('dialog').getByText('模板内容预览')).toBeVisible()
    await page.getByRole('button', { name: '使用此模板' }).click()
    await expect(page.getByRole('heading', { name: '使用模板' })).toBeVisible()
    await page.getByRole('button', { name: '确认使用' }).click()
    await expect(page.getByText('已使用模板')).toBeVisible()

    await page.goto('/resource/expense')
    await page.getByRole('button', { name: '费用申请' }).click()
    await expect(page.getByRole('dialog').getByText('费用申请')).toBeVisible()
    await page.getByRole('button', { name: '取消' }).click()

    await expectNoRuntimeErrors(page, errors)
  })
})
