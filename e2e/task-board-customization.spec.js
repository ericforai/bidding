// Input: Playwright E2E suite for task board API fixtures, drawer readback, and status customization
// Output: regression coverage for seeded columns, content persistence, sanitizer, and progress updates
// Pos: e2e/ - Playwright end-to-end coverage
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { test, expect } from '@playwright/test'
import { authedJson, createAuthenticatedSession, createProjectFixture } from './support/project-fixtures.js'

async function bootstrapProject(page, label) {
  const session = await createAuthenticatedSession()
  const project = await createProjectFixture(session, label)
  await page.addInitScript(({ token, user }) => {
    sessionStorage.setItem('token', token)
    sessionStorage.setItem('user', JSON.stringify(user))
  }, session)
  const projectId = String(project.id)
  await page.goto(`/project/${projectId}`)
  await expect(page).toHaveURL(/\/project\/\d+$/)
  await expect(page.getByText('任务看板').first()).toBeVisible()
  return { session, projectId }
}

async function createProjectTaskFixture(session, projectId, name, content = '') {
  const payload = await authedJson(`/api/projects/${projectId}/tasks`, session.token, {
    method: 'POST',
    body: JSON.stringify({
      title: name,
      description: '',
      content,
      assigneeId: session.user.id,
      assigneeName: session.user.name,
      priority: 'MEDIUM',
      dueDate: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19),
    }),
  })
  expect(payload?.success).toBeTruthy()
  expect(payload?.data?.id).toBeTruthy()
  return payload.data
}

async function updateTaskContentFixture(session, task, content) {
  const payload = await authedJson(`/api/tasks/${task.id}`, session.token, {
    method: 'PUT',
    body: JSON.stringify({
      title: task.title || task.name,
      description: task.description || '',
      content,
      status: String(task.status || 'TODO').replace('doing', 'IN_PROGRESS').replace('done', 'COMPLETED').toUpperCase(),
      priority: String(task.priority || 'MEDIUM').toUpperCase(),
      dueDate: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19),
    }),
  })
  expect(payload?.success).toBeTruthy()
  expect(payload?.data?.id).toBe(task.id)
  return payload.data
}

test.describe('Task board customization core flow', () => {
  test('drawer create → edit preserves content → status change updates progress', async ({ page }) => {
    const { session, projectId } = await bootstrapProject(page, '任务看板定制')

    const markdownContent = '## 任务步骤\n- 步骤1\n- 步骤2\n- 步骤3'

    // --- 1. Create through the real project task API, then assert the seeded
    //        status dictionary gives the board a visible TODO column/card. ---
    await createProjectTaskFixture(session, projectId, 'E2E 自动化测试任务', markdownContent)
    await page.reload()
    await expect(page.getByText('任务看板').first()).toBeVisible()
    // Scope to inner board column cards (`.column-content .task-card`) so we
    // don't accidentally match the outer `el-card.task-card` wrapper, whose
    // click does nothing.
    const createdCard = page.locator('.column-content .task-card').filter({ hasText: 'E2E 自动化测试任务' }).first()
    await expect(createdCard).toBeVisible()

    // --- 2. Re-open the drawer in edit mode, write content via the edit
    //        path (which exercises the new PUT /api/tasks/{id}), then assert
    //        round-trip persistence on reopen. ---
    await createdCard.click()

    let editDrawer = page.locator('.el-drawer').filter({ hasText: '编辑任务' })
    await expect(editDrawer).toBeVisible()

    await editDrawer.locator('textarea').first().fill(markdownContent)
    await editDrawer.getByRole('button', { name: '保存' }).click()
    await expect(editDrawer).toBeHidden()

    // Reopen and verify content survived the edit save (in-memory round-trip).
    await page.locator('.column-content .task-card').filter({ hasText: 'E2E 自动化测试任务' }).first().click()
    editDrawer = page.locator('.el-drawer').filter({ hasText: '编辑任务' })
    await expect(editDrawer).toBeVisible()

    const persistedValue = await editDrawer.locator('textarea').first().inputValue()
    expect(persistedValue).toContain('## 任务步骤')
    expect(persistedValue).toContain('- 步骤1')
    // Critical: the V102 content column + sanitizer must preserve line breaks
    // across the edit → reopen round-trip. Collapsing to a single line would
    // regress the Markdown experience the TaskForm advertises.
    expect(persistedValue).toContain('\n')

    await editDrawer.getByRole('button', { name: '取消' }).click()
    await expect(editDrawer).toBeHidden()

    // --- 3. Move the task to "已完成" via the card dropdown and verify progress ---
    const progressTag = page.locator('.el-tag').filter({ hasText: /^总进度:/ }).first()
    await expect(progressTag).toBeVisible()

    const cardForStatus = page.locator('.column-content .task-card').filter({ hasText: 'E2E 自动化测试任务' }).first()
    await cardForStatus.locator('.more-icon').first().click()
    // Dropdown items are rendered as "设为{name}"; target the COMPLETED terminal status.
    const completeMenuItem = page.locator('.el-dropdown-menu__item', { hasText: '设为已完成' }).first()
    await expect(completeMenuItem).toBeVisible()
    await completeMenuItem.click()

    // Progress tag should reflect the terminal transition (100% for a single task).
    await expect(progressTag).toContainText('100%')

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

  // N1: Real persistence proof — content survives a full page reload (not just
  // in-memory state). This guards the V102 content column + the new
  // PUT /api/tasks/{id} edit path together.
  test('content survives page reload (real persistence proof)', async ({ page }) => {
    const { session, projectId } = await bootstrapProject(page, '刷新闭环')

    const task = await createProjectTaskFixture(session, projectId, '刷新持久化-N1验证')
    await page.reload()
    await expect(page.getByText('任务看板').first()).toBeVisible()
    const card = page.locator('.column-content .task-card').filter({ hasText: '刷新持久化-N1验证' }).first()
    await expect(card).toBeVisible()

    const md = '## 步骤\n- 步骤A\n- 步骤B\n```ts\nconst x = 1\n```'
    await updateTaskContentFixture(session, task, md)

    // RELOAD — the critical step. After this, all in-memory state is gone;
    // anything we read came from the backend.
    await page.reload()
    await expect(page.getByText('任务看板').first()).toBeVisible()
    const cardAfterReload = page.locator('.column-content .task-card').filter({ hasText: '刷新持久化-N1验证' }).first()
    await expect(cardAfterReload).toBeVisible()

    // Reopen drawer in edit mode; content should still be there.
    await cardAfterReload.click()
    const editDrawer = page.locator('.el-drawer').filter({ hasText: '编辑任务' })
    await expect(editDrawer).toBeVisible()
    const value = await editDrawer.locator('textarea').first().inputValue()
    expect(value).toContain('## 步骤')
    expect(value).toContain('- 步骤B')
    expect(value).toContain('```ts')
    // Line breaks must survive the sanitizer + V102 column round-trip.
    expect(value).toContain('\n')
    await editDrawer.getByRole('button', { name: '取消' }).click()
    await expect(editDrawer).toBeHidden()
  })

  // N1: Sanitizer contract — control characters (e.g. BEL 0x07) must be
  // stripped on the way in, while real line breaks survive the same round-trip.
  test('control chars stripped while line breaks survive backend round-trip', async ({ page }) => {
    const { session, projectId } = await bootstrapProject(page, '控制字符闭环')

    const task = await createProjectTaskFixture(session, projectId, '控制字符-N1验证')
    await page.reload()
    await expect(page.getByText('任务看板').first()).toBeVisible()
    const card = page.locator('.column-content .task-card').filter({ hasText: '控制字符-N1验证' }).first()
    await expect(card).toBeVisible()

    // Write a payload containing 0x07 (BEL) plus a real newline through the
    // real update API, then verify the sanitized value through the UI.
    await updateTaskContentFixture(session, task, 'beforeafter\nnext-line')

    // Reload to bypass any local-state masking — we must read what the backend
    // actually persisted.
    await page.reload()
    await expect(page.getByText('任务看板').first()).toBeVisible()
    const cardAfterReload = page.locator('.column-content .task-card').filter({ hasText: '控制字符-N1验证' }).first()
    await expect(cardAfterReload).toBeVisible()

    await cardAfterReload.click()
    const editDrawer = page.locator('.el-drawer').filter({ hasText: '编辑任务' })
    await expect(editDrawer).toBeVisible()
    const value = await editDrawer.locator('textarea').first().inputValue()
    // BEL must be stripped by the sanitizer; the real newline must survive.
    expect(value).not.toContain('')
    expect(value).toContain('\n')
    expect(value).toContain('before')
    expect(value).toContain('after')
    expect(value).toContain('next-line')
    await editDrawer.getByRole('button', { name: '取消' }).click()
    await expect(editDrawer).toBeHidden()
  })
})
