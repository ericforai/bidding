import { expect, test } from '@playwright/test'
import { ensureApiSession, injectSession } from './auth-helpers.js'

async function loginAsStyleRegressionUser(page) {
  const session = await ensureApiSession({
    username: `form_style_${Date.now()}`,
    role: 'ADMIN',
    fullName: 'Form Style Admin',
  })
  await injectSession(page, session)
}

async function readBoxStyle(locator) {
  return locator.evaluate((element) => {
    const style = window.getComputedStyle(element)
    return {
      borderColor: style.borderColor,
      boxShadow: style.boxShadow,
      height: style.height,
      outline: style.outline,
      transition: style.transition,
      width: window.getComputedStyle(element.closest('.el-input, .el-select') || element).width,
    }
  })
}

test('form controls keep simple mouse focus and keyboard-only gray focus affordance', async ({ page }) => {
  await loginAsStyleRegressionUser(page)
  await page.goto('/project')
  await expect(page.locator('.search-form')).toBeVisible()

  const nameInput = page.getByPlaceholder('请输入项目名称')
  const nameWrapper = nameInput.locator('xpath=ancestor::div[contains(@class, "el-input__wrapper")]')

  await nameInput.click()
  await expect(await readBoxStyle(nameWrapper)).toMatchObject({
    borderColor: 'rgb(208, 208, 208)',
    boxShadow: 'none',
    height: '40px',
    outline: 'rgb(26, 26, 26) none 0px',
    transition: 'none',
    width: '168px',
  })

  await page.keyboard.press('Tab')
  await nameInput.focus()
  await expect(await readBoxStyle(nameWrapper)).toMatchObject({
    borderColor: 'rgb(176, 176, 176)',
    boxShadow: 'none',
    outline: 'rgb(26, 26, 26) none 0px',
  })

  await nameInput.click()
  await expect(await readBoxStyle(nameWrapper)).toMatchObject({
    borderColor: 'rgb(208, 208, 208)',
    boxShadow: 'none',
  })

  const selectWrapper = page.locator('.search-form .el-select__wrapper').first()
  await selectWrapper.click()
  await expect(await readBoxStyle(selectWrapper)).toMatchObject({
    borderColor: 'rgb(208, 208, 208)',
    boxShadow: 'none',
    height: '40px',
    outline: 'rgb(26, 26, 26) none 0px',
    transition: 'none',
    width: '168px',
  })
})
