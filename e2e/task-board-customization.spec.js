// Input: Playwright E2E suite for the task board drawer + status customization flow
// Output: regression coverage for Task I1 (create/edit/status-change/progress) via real API
// Pos: e2e/ - Playwright end-to-end coverage
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test'
import { authedJson, createAuthenticatedSession, createProjectFixture } from './support/project-fixtures.js'

test.describe('Task board customization core flow', () => {
  test('drawer create → edit preserves content → status change updates progress', async ({ page }) => {
    const session = await createAuthenticatedSession()
    const project = await createProjectFixture(session, '任务看板定制')

    await page.addInitScript(({ token, user }) => {
      sessionStorage.setItem('token', token)
      sessionStorage.setItem('user', JSON.stringify(user))
    }, session)

    const projectId = String(project.id)

    await page.goto(`/project/${projectId}`)
    await expect(page).toHaveURL(/\/project\/\d+$/)
    await expect(page.getByText('任务看板').first()).toBeVisible()

    // --- 1. Open the create drawer via the header button and fill TaskForm ---
    // Element Plus `el-button` with `link` prop does not forward `data-test`
    // attrs to the native button, so we rely on the accessible name here
    // (matching the pattern used by project-detail-workflow.spec.js).
    await page.getByRole('button', { name: '添加任务' }).click()

    const drawer = page.locator('.el-drawer').filter({ hasText: '新增任务' })
    await expect(drawer).toBeVisible()

    const markdownContent = '## 任务步骤\n- 步骤1\n- 步骤2\n- 步骤3'

    // TaskForm renders labelled form items; scope inputs to the drawer.
    await drawer.locator('input[placeholder="请输入任务名称"]').fill('E2E 自动化测试任务')
    await drawer.locator('textarea').first().fill(markdownContent)

    await drawer.getByRole('button', { name: '保存' }).click()

    // Drawer closes and the created card becomes visible on the board.
    await expect(drawer).toBeHidden()
    const createdCard = page.locator('.task-card').filter({ hasText: 'E2E 自动化测试任务' }).first()
    await expect(createdCard).toBeVisible()

    // --- 2. Re-open the drawer in edit mode and assert content round-trip (B2 regression) ---
    await createdCard.click()

    const editDrawer = page.locator('.el-drawer').filter({ hasText: '编辑任务' })
    await expect(editDrawer).toBeVisible()

    const persistedValue = await editDrawer.locator('textarea').first().inputValue()
    expect(persistedValue).toContain('## 任务步骤')
    expect(persistedValue).toContain('- 步骤1')
    // Critical: the V102 content column + sanitizer must preserve line breaks
    // across the create → reopen round-trip. Collapsing to a single line
    // would regress the Markdown experience the TaskForm advertises.
    expect(persistedValue).toContain('\n')

    await editDrawer.getByRole('button', { name: '取消' }).click()
    await expect(editDrawer).toBeHidden()

    // --- 3. Move the task to "已完成" via the card dropdown and verify progress ---
    // Capture baseline progress so we can assert movement after the status change.
    const progressTag = page.locator('.el-tag').filter({ hasText: /^总进度:/ }).first()
    await expect(progressTag).toBeVisible()
    const initialProgress = (await progressTag.textContent())?.trim()

    await createdCard.locator('.more-icon').click()
    // Dropdown items are rendered as "设为{name}"; target the COMPLETED terminal status.
    const completeMenuItem = page.locator('.el-dropdown-menu__item', { hasText: '设为已完成' }).first()
    await expect(completeMenuItem).toBeVisible()
    await completeMenuItem.click()

    // Progress tag should reflect the terminal transition (100% for a single task).
    await expect(progressTag).toContainText('100%')
    if (initialProgress) {
      expect((await progressTag.textContent())?.trim()).not.toBe(initialProgress)
    }

    // --- 4. Back-channel assertion: the backend persisted the task and status ---
    const tasksPayload = await authedJson(`/api/projects/${projectId}/tasks`, session.token)
    expect(tasksPayload?.success).toBeTruthy()
    const persisted = (tasksPayload?.data || []).find((t) => t.name === 'E2E 自动化测试任务')
    expect(persisted).toBeTruthy()
    // The status dict uses the COMPLETED code for the terminal column; accept
    // either the canonical code or the legacy "done" literal if the backend
    // normalizes on the way out.
    expect(String(persisted.status || '').toUpperCase()).toMatch(/COMPLETED|DONE/)
  })
})
