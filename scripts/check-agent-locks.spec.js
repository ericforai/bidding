import { describe, expect, it } from 'vitest'

import {
  checkAgentLocks,
  collectLocksFromRegistries,
  findBlockingLocks,
  isExpiredLock,
  loadAgentContext,
  parseAgentLocks,
  parsePorcelainUntrackedFiles,
  pathMatchesLock,
  resolveCurrentBranch,
} from './check-agent-locks.mjs'

const now = new Date('2026-05-03T12:00:00+08:00')

describe('check-agent-locks', () => {
  it('parses a minimal lock registry', () => {
    expect(
      parseAgentLocks(`
version: 1
locks:
  - path: src/views/Project/Detail.vue
    scope: file
    owner: claude
    branch: claude/project-detail
    task: project-detail
    expiresAt: 2026-05-05T23:59:59+08:00
    reason: 项目详情页重构
`),
    ).toEqual({
      version: 1,
      locks: [
        {
          path: 'src/views/Project/Detail.vue',
          scope: 'file',
          owner: 'claude',
          branch: 'claude/project-detail',
          task: 'project-detail',
          expiresAt: '2026-05-05T23:59:59+08:00',
          reason: '项目详情页重构',
        },
      ],
    })
  })

  it('parses quoted scalars that contain yaml comment characters', () => {
    expect(
      parseAgentLocks(`
version: 1
locks:
  - path: "src/views/Project/Detail.vue"
    scope: "file"
    owner: "codex"
    branch: "codex/agent-lock-gate"
    task: "agent-lock-gate"
    expiresAt: "2026-05-04T04:00:00.000Z"
    reason: "项目详情页: 改造 #1"
`),
    ).toMatchObject({
      locks: [
        {
          reason: '项目详情页: 改造 #1',
        },
      ],
    })
  })

  it('loads agent context from key value lines', () => {
    expect(
      loadAgentContext(`
agent=codex
task=agent-lock-gate
branch=codex/agent-lock-gate
base=origin/main
worktree=/Users/user/xiyu/worktrees/codex-agent-lock-gate
`),
    ).toMatchObject({
      agent: 'codex',
      task: 'agent-lock-gate',
      branch: 'codex/agent-lock-gate',
      base: 'origin/main',
    })
  })

  it('loads agent context from legacy json files', () => {
    expect(
      loadAgentContext(`{
  "agent": "codex",
  "branch": "codex/old-task",
  "task": "旧任务"
}`),
    ).toMatchObject({
      agent: 'codex',
      branch: 'codex/old-task',
      task: '旧任务',
    })
  })

  it('matches file and directory locks', () => {
    expect(
      pathMatchesLock('src/views/Project/Detail.vue', {
        path: 'src/views/Project/Detail.vue',
        scope: 'file',
      }),
    ).toBe(true)
    expect(
      pathMatchesLock('src/views/Project/components/TaskBoard.vue', {
        path: 'src/views/Project',
        scope: 'directory',
      }),
    ).toBe(true)
    expect(
      pathMatchesLock('src/views/Projector/Detail.vue', {
        path: 'src/views/Project',
        scope: 'directory',
      }),
    ).toBe(false)
  })

  it('blocks another agent from changing an active locked file', () => {
    const blockingLocks = findBlockingLocks({
      changedFiles: ['src/views/Project/Detail.vue'],
      context: { agent: 'codex', branch: 'codex/project-detail-fix' },
      locks: [
        {
          path: 'src/views/Project/Detail.vue',
          scope: 'file',
          owner: 'claude',
          branch: 'claude/project-detail',
          task: 'project-detail',
          expiresAt: '2026-05-05T23:59:59+08:00',
          reason: '项目详情页重构',
        },
      ],
      now,
    })

    expect(blockingLocks).toHaveLength(1)
    expect(blockingLocks[0]).toMatchObject({
      file: 'src/views/Project/Detail.vue',
      lock: { owner: 'claude' },
    })
  })

  it('allows the lock owner to change the locked file', () => {
    expect(
      findBlockingLocks({
        changedFiles: ['src/views/Project/Detail.vue'],
        context: { agent: 'claude', branch: 'claude/project-detail' },
        locks: [
          {
            path: 'src/views/Project/Detail.vue',
            scope: 'file',
            owner: 'claude',
            branch: 'claude/project-detail',
            task: 'project-detail',
            expiresAt: '2026-05-05T23:59:59+08:00',
            reason: '项目详情页重构',
          },
        ],
        now,
      }),
    ).toEqual([])
  })

  it('blocks the same agent name on a different task branch', () => {
    const blockingLocks = findBlockingLocks({
      changedFiles: ['src/views/Project/Detail.vue'],
      context: { agent: 'codex', branch: 'codex/another-task' },
      locks: [
        {
          path: 'src/views/Project/Detail.vue',
          scope: 'file',
          owner: 'codex',
          branch: 'codex/project-detail',
          task: 'project-detail',
          expiresAt: '2026-05-05T23:59:59+08:00',
          reason: '同一 Agent 的另一个任务锁',
        },
      ],
      now,
    })

    expect(blockingLocks).toHaveLength(1)
  })

  it('ignores expired locks for blocking decisions', () => {
    const lock = {
      path: 'src/views/Project/Detail.vue',
      scope: 'file',
      owner: 'claude',
      branch: 'claude/project-detail',
      task: 'project-detail',
      expiresAt: '2026-05-02T23:59:59+08:00',
      reason: '项目详情页重构',
    }

    expect(isExpiredLock(lock, now)).toBe(true)
    expect(
      findBlockingLocks({
        changedFiles: ['src/views/Project/Detail.vue'],
        context: { agent: 'codex', branch: 'codex/project-detail-fix' },
        locks: [lock],
        now,
      }),
    ).toEqual([])
  })

  it('fails duplicate active locks on the same path and scope', () => {
    const result = checkAgentLocks({
      changedFiles: [],
      context: { agent: 'codex', branch: 'codex/agent-lock-gate' },
      locks: [
        {
          path: 'src/views/Project/Detail.vue',
          scope: 'file',
          owner: 'claude',
          branch: 'claude/project-detail',
          task: 'project-detail',
          expiresAt: '2026-05-05T23:59:59+08:00',
          reason: '项目详情页重构',
        },
        {
          path: 'src/views/Project/Detail.vue',
          scope: 'file',
          owner: 'gemini',
          branch: 'gemini/project-detail',
          task: 'project-detail-copy',
          expiresAt: '2026-05-05T23:59:59+08:00',
          reason: '重复锁',
        },
      ],
      now,
    })

    expect(result.ok).toBe(false)
    expect(result.duplicateLocks).toHaveLength(1)
  })

  it('merges lock registries from remote branches and preserves their source', () => {
    const locks = collectLocksFromRegistries([
      {
        source: 'working-tree',
        registry: {
          locks: [
            {
              path: 'src/views/Project/Detail.vue',
              scope: 'file',
              owner: 'codex',
              branch: 'codex/agent-lock-gate',
              task: 'agent-lock-gate',
              expiresAt: '2026-05-05T23:59:59+08:00',
              reason: '本分支锁',
            },
          ],
        },
      },
      {
        source: 'origin/claude/project-detail',
        registry: {
          locks: [
            {
              path: 'src/views/Project/components',
              scope: 'directory',
              owner: 'claude',
              branch: 'claude/project-detail',
              task: 'project-detail',
              expiresAt: '2026-05-05T23:59:59+08:00',
              reason: '远端分支锁',
            },
          ],
        },
      },
    ], { currentBranch: 'codex/agent-lock-gate' })

    expect(locks).toEqual([
      expect.objectContaining({ owner: 'codex', source: 'working-tree' }),
      expect.objectContaining({ owner: 'claude', source: 'origin/claude/project-detail' }),
    ])
  })

  it('ignores inherited locks that do not belong to their source branch', () => {
    const locks = collectLocksFromRegistries(
      [
        {
          source: 'working-tree',
          registry: {
            locks: [
              {
                path: 'src/views/Project/Detail.vue',
                scope: 'file',
                owner: 'claude',
                branch: 'claude/old-task',
                task: 'old-task',
                expiresAt: '2026-05-05T23:59:59+08:00',
                reason: '从 main 继承来的旧锁',
              },
            ],
          },
        },
        {
          source: 'origin/codex/current-task',
          registry: {
            locks: [
              {
                path: 'src/views/Project/Detail.vue',
                scope: 'file',
                owner: 'claude',
                branch: 'claude/old-task',
                task: 'old-task',
                expiresAt: '2026-05-05T23:59:59+08:00',
                reason: '远端分支继承来的旧锁',
              },
            ],
          },
        },
        {
          source: 'origin/claude/current-task',
          registry: {
            locks: [
              {
                path: 'src/views/Project/components',
                scope: 'directory',
                owner: 'claude',
                branch: 'claude/current-task',
                task: 'current-task',
                expiresAt: '2026-05-05T23:59:59+08:00',
                reason: '真正属于该远端分支的锁',
              },
            ],
          },
        },
      ],
      { currentBranch: 'codex/current-task' },
    )

    expect(locks).toEqual([expect.objectContaining({ branch: 'claude/current-task' })])
  })

  it('parses untracked files from porcelain status output', () => {
    expect(
      parsePorcelainUntrackedFiles(` M package.json
?? .agent-locks.yml
?? scripts/fixtures/
A  scripts/tracked-file.mjs
`),
    ).toEqual(['.agent-locks.yml', 'scripts/fixtures/'])
  })

  it('resolves GitHub pull request head branch when Git checkout is detached', () => {
    expect(
      resolveCurrentBranch('', {
        GITHUB_HEAD_REF: 'codex/agent-lock-gate',
        GITHUB_REF_NAME: '12/merge',
      }),
    ).toBe('codex/agent-lock-gate')
  })
})
