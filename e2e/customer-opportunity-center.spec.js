import { test, expect } from '@playwright/test'

async function loginAsMockUser(page) {
  await page.goto('/login')
  await page.evaluate(() => {
    Object.keys(window.localStorage)
      .filter((key) => key.startsWith('xiyu-demo:'))
      .forEach((key) => window.localStorage.removeItem(key))
  })
  await page.getByPlaceholder('请输入用户名').fill('小王')
  await page.getByPlaceholder('请输入密码').fill('123456')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
}

test.describe('customer opportunity center', () => {
  test('sales can open customer opportunity center and jump into project creation', async ({ page }) => {
    await loginAsMockUser(page)

    await page.goto('/bidding')
    await expect(page.getByRole('button', { name: '客户商机中心' })).toBeVisible()
    await page.getByRole('button', { name: '客户商机中心' }).click()

    await expect(page).toHaveURL(/\/bidding\/customer-opportunities$/)
    await expect(page.getByRole('heading', { name: '客户商机中心' })).toBeVisible()

    await page.getByText('国家电网江苏省电力有限公司').first().click()
    await expect(page.getByRole('heading', { name: '预测商机' })).toBeVisible()

    await page.getByRole('button', { name: '转为正式项目' }).click()

    await expect(page).toHaveURL(/\/project\/create/)
    await expect(page.locator('input[placeholder="请输入客户名称"]')).toHaveValue(/国家电网江苏省电力有限公司/)
    await expect(page.locator('input[placeholder="请输入项目名称"]')).toHaveValue(/项目|采购/)
    await expect(page.locator('input[placeholder="请选择日期"]').first()).toHaveValue('2025-03-28')

    await page.getByRole('button', { name: '下一步' }).click()
    await page.getByRole('button', { name: '下一步' }).click()
    await page.getByRole('button', { name: '下一步' }).click()
    await page.getByRole('button', { name: '确认并创建项目' }).click()

    await expect(page).toHaveURL(/\/project\/P/)

    await page.goto('/bidding/customer-opportunities')
    await page.getByText('国家电网江苏省电力有限公司').first().click()
    await expect(page.getByRole('button', { name: '查看已转项目' })).toBeVisible()
  })
})
