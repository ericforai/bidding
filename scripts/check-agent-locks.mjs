#!/usr/bin/env node
// Input: .agent-locks.yml, current agent context, and a Git diff range
// Output: exits non-zero when current changes touch another Agent's active lock
// Pos: scripts/多 Agent 文件锁门禁
// 维护声明: 若锁字段、Agent 上下文格式或分支同步规则变化，请同步更新 AGENTS.md、RULES.md 和 scripts/README.md。
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

import { loadCombinedLocks } from './lib/agent-lock-store.mjs'

const DEFAULT_LOCK_FILE = '.agent-locks.yml'
const DEFAULT_HOT_PATHS_FILE = 'scripts/hot-paths.yml'

export function parseAgentLocks(content) {
  const lines = content.split(/\r?\n/)
  const registry = { version: 1, locks: [] }
  let currentLock = null

  for (const rawLine of lines) {
    const line = stripYamlComment(rawLine)
    const trimmed = line.trim()
    if (!trimmed) continue

    if (trimmed.startsWith('version:')) {
      registry.version = Number.parseInt(readScalar(trimmed.slice('version:'.length)), 10)
      continue
    }

    if (trimmed === 'locks:' || trimmed === 'locks: []') {
      continue
    }

    if (line.startsWith('  - ')) {
      currentLock = {}
      registry.locks.push(currentLock)
      assignYamlField(currentLock, trimmed.slice(2))
      continue
    }

    if (line.startsWith('    ') && currentLock) {
      assignYamlField(currentLock, trimmed)
    }
  }

  return registry
}

export function loadAgentContext(content) {
  const trimmedContent = content.trim()
  if (trimmedContent.startsWith('{')) {
    try {
      return JSON.parse(trimmedContent)
    } catch {
      return {}
    }
  }

  return content
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .reduce((context, line) => {
      const separatorIndex = line.indexOf('=')
      if (separatorIndex === -1) return context
      context[line.slice(0, separatorIndex)] = line.slice(separatorIndex + 1)
      return context
    }, {})
}

export function pathMatchesLock(filePath, lock) {
  const normalizedFile = normalizeRepoPath(filePath)
  const normalizedLockPath = normalizeRepoPath(lock.path)

  if (lock.scope === 'file') {
    return normalizedFile === normalizedLockPath
  }

  if (lock.scope === 'directory') {
    return normalizedFile === normalizedLockPath || normalizedFile.startsWith(`${normalizedLockPath}/`)
  }

  return false
}

export function isExpiredLock(lock, now = new Date()) {
  if (!lock.expiresAt) return false
  const expiresAt = new Date(lock.expiresAt)
  return Number.isNaN(expiresAt.getTime()) ? false : expiresAt < now
}

export function findBlockingLocks({ changedFiles, context, locks, now = new Date() }) {
  return changedFiles.flatMap((file) =>
    locks
      .filter((lock) => !isExpiredLock(lock, now))
      .filter((lock) => pathMatchesLock(file, lock))
      .filter((lock) => !isLockOwnedByContext(lock, context))
      .map((lock) => ({ file, lock })),
  )
}

export function checkAgentLocks({ changedFiles, context, locks, now = new Date(), hotPaths = [], selfMergeOrphans = [] }) {
  const duplicateLocks = findDuplicateActiveLocks(locks, now)
  const blockingLocks = findBlockingLocks({ changedFiles, context, locks, now })
  const expiredLocks = locks.filter((lock) => isExpiredLock(lock, now))
  const missingHotPathLocks = findMissingHotPathLocks({ changedFiles, context, locks, now, hotPaths })

  return {
    ok:
      duplicateLocks.length === 0 &&
      blockingLocks.length === 0 &&
      missingHotPathLocks.length === 0 &&
      selfMergeOrphans.length === 0,
    blockingLocks,
    duplicateLocks,
    expiredLocks,
    missingHotPathLocks,
    selfMergeOrphans,
  }
}

// Branches allowed to land lock-file changes onto main without triggering the
// self-merge gate. These workflows manage the lock store as their job —
// expecting them to release-before-merge would be circular.
const SELF_MERGE_WHITELIST_PATTERNS = [
  /^chore\/janitor-/,
  /^chore\/clean-orphan-lock/,
  /^chore\/auto-release-/,
]

function isWhitelistedForSelfMerge(branch) {
  return SELF_MERGE_WHITELIST_PATTERNS.some((rx) => rx.test(branch || ''))
}

/**
 * Detect locks that a PR adds and that name the PR's own head branch.
 * Squash-merging such a PR carries the lock entry into main forever — once
 * the PR head is closed (or auto-deleted), the lock becomes orphaned. Block
 * the PR at CI time so it must `npm run agent:lock-release --all` before merge.
 *
 * Exception: a lock that covers a hot-path file the PR is actually changing
 * is REQUIRED by the existing hot-path gate. Without this carve-out, any PR
 * touching .github/workflows/ etc. would be impossible to merge. L1/L2
 * continue to clean such locks after the squash-merge lands on main.
 *
 * @param {object} input
 * @param {Array<{branch?: string, path?: string, scope?: string, source?: string}>} input.addedLocks - locks introduced by the PR diff (after - before)
 * @param {string} input.prHeadBranch - PR head branch (e.g. claude/some-task)
 * @param {string} input.prBaseBranch - PR base branch; only `main` triggers the check
 * @param {Array<string>} [input.changedHotPathFiles] - PR-changed files that match hot-path patterns
 * @returns {Array<object>} locks that should block the merge
 */
export function findSelfMergeOrphans({ addedLocks, prHeadBranch, prBaseBranch, changedHotPathFiles = [] }) {
  if (prBaseBranch !== 'main') return []
  if (!prHeadBranch) return []
  if (isWhitelistedForSelfMerge(prHeadBranch)) return []
  return (addedLocks || [])
    .filter((lock) => lock.branch === prHeadBranch)
    .filter((lock) => !changedHotPathFiles.some((file) => pathMatchesLock(file, lock)))
}

export function collectLocksFromRegistries(sources, { currentBranch = '' } = {}) {
  return sources.flatMap(({ source, registry }) =>
    (registry.locks || [])
      .map((lock) => ({
        ...lock,
        source,
      }))
      .filter((lock) => lockBelongsToSource(lock, source, currentBranch)),
  )
}

export function parsePorcelainUntrackedFiles(output) {
  return output
    .split(/\r?\n/)
    .filter((line) => line.startsWith('?? '))
    .map((line) => line.slice(3).trim())
    .filter(Boolean)
}

function assignYamlField(target, fieldLine) {
  const separatorIndex = fieldLine.indexOf(':')
  if (separatorIndex === -1) return
  const key = fieldLine.slice(0, separatorIndex).trim()
  target[key] = readScalar(fieldLine.slice(separatorIndex + 1))
}

function readScalar(value) {
  const trimmed = value.trim()
  if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
    try {
      return JSON.parse(trimmed)
    } catch {
      return trimmed.slice(1, -1)
    }
  }
  if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
    return trimmed.slice(1, -1)
  }
  return trimmed
}

function stripYamlComment(line) {
  let inDoubleQuote = false
  let inSingleQuote = false
  let previousChar = ''

  for (let index = 0; index < line.length; index += 1) {
    const char = line[index]
    if (char === '"' && !inSingleQuote && previousChar !== '\\') {
      inDoubleQuote = !inDoubleQuote
    } else if (char === "'" && !inDoubleQuote) {
      inSingleQuote = !inSingleQuote
    } else if (char === '#' && !inDoubleQuote && !inSingleQuote && /\s/.test(previousChar)) {
      return line.slice(0, index).trimEnd()
    }
    previousChar = char
  }

  return line
}

function normalizeRepoPath(filePath) {
  return filePath.replaceAll('\\', '/').replace(/^\.\//, '').replace(/\/+$/, '')
}

function isLockOwnedByContext(lock, context = {}) {
  return Boolean(context.branch && lock.branch === context.branch)
}

function findDuplicateActiveLocks(locks, now) {
  const activeLocksByKey = new Map()
  const duplicates = []

  for (const lock of locks) {
    if (isExpiredLock(lock, now)) continue
    const key = `${lock.scope}:${normalizeRepoPath(lock.path)}`
    const existingLock = activeLocksByKey.get(key)
    if (existingLock && !isSameLogicalLock(existingLock, lock)) {
      duplicates.push({ key, locks: [existingLock, lock] })
    } else if (!existingLock) {
      activeLocksByKey.set(key, lock)
    }
  }

  return duplicates
}

function findMissingHotPathLocks({ changedFiles, context, locks, now, hotPaths }) {
  if (!hotPaths || hotPaths.length === 0) return []

  const activeLocks = locks.filter((lock) => !isExpiredLock(lock, now))
  const myLocks = activeLocks.filter((lock) => isLockOwnedByContext(lock, context))
  const missing = []

  for (const file of changedFiles) {
    const matchedHotPath = hotPaths.find((hp) => fileMatchesHotPathPattern(file, hp.pattern))
    if (!matchedHotPath) continue

    const coveredByMyLock = myLocks.some((lock) => pathMatchesLock(file, lock))
    if (!coveredByMyLock) {
      missing.push({ file, hotPath: matchedHotPath })
    }
  }

  return missing
}

function fileMatchesHotPathPattern(filePath, pattern) {
  const normalized = normalizeRepoPath(filePath)
  const patternNormalized = normalizeRepoPath(pattern)

  if (patternNormalized.endsWith('/**')) {
    const prefix = patternNormalized.slice(0, -3)
    return normalized === prefix || normalized.startsWith(`${prefix}/`)
  }

  if (patternNormalized.includes('*')) {
    const regex = new RegExp('^' + patternNormalized.replace(/\*/g, '.*') + '$')
    return regex.test(normalized)
  }

  return normalized === patternNormalized
}

function isSameLogicalLock(left, right) {
  return left.owner === right.owner && left.branch === right.branch && left.task === right.task
}

/**
 * Diff two lock-set arrays to find entries present in `after` but not in `before`.
 * Identity = (scope, path, owner, branch). Pure function, easy to test.
 */
export function computeAddedLocks({ before, after }) {
  const beforeKeys = new Set(
    (before || []).map((l) => `${l.scope}\0${l.path}\0${l.owner || ''}\0${l.branch || ''}`),
  )
  return (after || []).filter(
    (l) => !beforeKeys.has(`${l.scope}\0${l.path}\0${l.owner || ''}\0${l.branch || ''}`),
  )
}

function main() {
  const options = parseArgs(process.argv.slice(2))
  const rootDir = runGit(['rev-parse', '--show-toplevel']).trim()
  // Working-tree locks union: legacy `.agent-locks.yml` + per-task `.agent-locks/*.yml`.
  // The new per-task scheme avoids merge conflicts on the single legacy file.
  const localLocks = loadCombinedLocks({ rootDir, lockFile: options.lockFile })
  const localRegistry = { version: 1, locks: localLocks.map(({ source: _ignored, ...lock }) => lock) }
  const registrySources = [{ source: 'working-tree', registry: localRegistry }]
  if (options.includeRemoteLocks) {
    registrySources.push(...readRemoteLockRegistries(options.lockFile))
  }
  const context = loadContextFromWorktree(rootDir)
  const locks = collectLocksFromRegistries(registrySources, { currentBranch: context.branch })
  const changedFiles = options.changedFiles ?? readChangedFiles({ base: options.base, head: options.head })
  const hotPaths = loadHotPaths(path.join(rootDir, DEFAULT_HOT_PATHS_FILE))

  // R3 — self-merge orphan gate. Only meaningful in PR context where we know
  // base and head. Skip when running locally (--changed-only) or when env
  // lacks PR metadata.
  const changedHotPathFiles = changedFiles.filter((file) =>
    hotPaths.some((hp) => fileMatchesHotPathPattern(file, hp.pattern)),
  )
  const selfMergeOrphans = computeSelfMergeOrphansForRun({
    base: options.base,
    head: options.head,
    prHeadBranch: process.env.GITHUB_HEAD_REF || '',
    prBaseBranch: process.env.GITHUB_BASE_REF || '',
    changedHotPathFiles,
  })

  const result = checkAgentLocks({
    changedFiles,
    context,
    locks,
    hotPaths,
    selfMergeOrphans,
  })

  printWarnings(result, context)

  if (!result.ok) {
    printFailures(result, context)
    process.exit(1)
  }

  console.log(
    `agent-lock-check: ok (${changedFiles.length} changed file${changedFiles.length === 1 ? '' : 's'}, ${locks.length} lock${locks.length === 1 ? '' : 's'})`,
  )
}

function computeSelfMergeOrphansForRun({ base, head, prHeadBranch, prBaseBranch, changedHotPathFiles = [] }) {
  if (!prHeadBranch || !prBaseBranch) return []
  if (prBaseBranch !== 'main') return []
  if (!head || head === null) return []

  const before = readLocksAtRev(base)
  const after = readLocksAtRev(head)
  const added = computeAddedLocks({ before, after })
  return findSelfMergeOrphans({ addedLocks: added, prHeadBranch, prBaseBranch, changedHotPathFiles })
}

function readLocksAtRev(rev) {
  if (!rev) return []
  const out = []
  // Legacy single file
  const legacy = runGit(['show', `${rev}:.agent-locks.yml`], { allowFailure: true })
  if (legacy.trim()) {
    const registry = parseAgentLocks(legacy)
    for (const lock of registry.locks || []) out.push({ ...lock, source: 'legacy' })
  }
  // Per-task directory — list files via ls-tree then read each
  const tree = runGit(['ls-tree', '--name-only', rev, '.agent-locks/'], { allowFailure: true })
  for (const entry of tree.split(/\r?\n/).map((s) => s.trim()).filter(Boolean)) {
    if (!entry.endsWith('.yml')) continue
    const content = runGit(['show', `${rev}:${entry}`], { allowFailure: true })
    if (!content.trim()) continue
    const registry = parseAgentLocks(content)
    const filename = entry.replace(/^.*\//, '')
    for (const lock of registry.locks || []) out.push({ ...lock, source: `per-task:${filename}` })
  }
  return out
}

function parseArgs(args) {
  const options = {
    base: 'origin/main',
    head: 'HEAD',
    lockFile: DEFAULT_LOCK_FILE,
    includeRemoteLocks: true,
  }

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index]
    if (arg === '--base') {
      options.base = args[++index]
    } else if (arg === '--head') {
      options.head = args[++index]
    } else if (arg === '--lock-file') {
      options.lockFile = args[++index]
    } else if (arg === '--changed-only') {
      options.base = 'HEAD'
      options.head = null
      options.includeUntracked = true
    } else if (arg === '--no-remote-locks') {
      options.includeRemoteLocks = false
    }
  }

  return options
}

function readRemoteLockRegistries(lockFile) {
  const refsOutput = runGit(['for-each-ref', '--format=%(refname:short)', 'refs/remotes/origin'], {
    allowFailure: true,
  })
  if (!refsOutput.trim()) return []

  return refsOutput
    .trim()
    .split(/\r?\n/)
    .filter((ref) => ref && ref !== 'origin/HEAD')
    .flatMap((ref) => {
      const content = runGit(['show', `${ref}:${lockFile}`], { allowFailure: true })
      if (!content.trim()) return []
      return [{ source: ref, registry: parseAgentLocks(content) }]
    })
}

function loadContextFromWorktree(rootDir) {
  const gitBranch = runGit(['symbolic-ref', '--quiet', '--short', 'HEAD'], { allowFailure: true }).trim()
  const branch = resolveCurrentBranch(gitBranch)
  const contextPath = path.join(rootDir, '.agent-task-context')
  if (fs.existsSync(contextPath)) {
    const context = loadAgentContext(fs.readFileSync(contextPath, 'utf8'))
    const agentFromBranch = branch.includes('/') ? branch.split('/')[0] : ''
    return {
      ...context,
      agent: agentFromBranch || context.agent,
      branch: branch || context.branch,
    }
  }

  return {
    agent: branch.includes('/') ? branch.split('/')[0] : '',
    branch,
  }
}

function loadHotPaths(hotPathsFile) {
  if (!fs.existsSync(hotPathsFile)) return []
  try {
    const content = fs.readFileSync(hotPathsFile, 'utf8')
    const lines = content.split(/\r?\n/)
    const hotPaths = []
    let currentHotPath = null

    for (const rawLine of lines) {
      const line = stripYamlComment(rawLine)
      const trimmed = line.trim()
      if (!trimmed) continue

      if (trimmed === 'hot_paths:' || trimmed === 'hot_paths: []') {
        continue
      }

      if (line.startsWith('  - ')) {
        currentHotPath = {}
        hotPaths.push(currentHotPath)
        assignYamlField(currentHotPath, trimmed.slice(2))
        continue
      }

      if (line.startsWith('    ') && currentHotPath) {
        assignYamlField(currentHotPath, trimmed)
      }
    }

    return hotPaths
  } catch {
    return []
  }
}

export function resolveCurrentBranch(gitBranch, env = process.env) {
  if (gitBranch) return gitBranch
  if (env.GITHUB_HEAD_REF) return env.GITHUB_HEAD_REF
  if (env.GITHUB_REF_NAME && !env.GITHUB_REF_NAME.endsWith('/merge')) return env.GITHUB_REF_NAME
  if (env.GITHUB_REF?.startsWith('refs/heads/')) return env.GITHUB_REF.slice('refs/heads/'.length)
  return ''
}

function lockBelongsToSource(lock, source, currentBranch) {
  if (!lock.branch) return false
  if (source === 'working-tree') {
    return !currentBranch || lock.branch === currentBranch
  }
  if (source.startsWith('origin/')) {
    return lock.branch === source.slice('origin/'.length)
  }
  return true
}

function readChangedFiles({ base, head }) {
  const args = head
    ? ['diff', '--name-status', '--diff-filter=ACMR', base, head]
    : ['diff', '--name-status', '--diff-filter=ACMR', base]
  const output = runGit(args, { allowFailure: true })
  const diffFiles = output
    .trim()
    .split(/\r?\n/)
    .flatMap((line) => parseDiffNameStatus(line))
    .filter(Boolean)
  const untrackedFiles = head ? [] : readUntrackedFiles()

  return [...new Set([...diffFiles, ...untrackedFiles])]
}

function parseDiffNameStatus(line) {
  const parts = line.split('\t')
  const statusCode = parts[0]?.[0]
  if (!statusCode) return []
  if (statusCode === 'R') return [parts[1], parts[2]]
  if (statusCode === 'C') return [parts[2]]
  return [parts[1]]
}

function runGit(args, { allowFailure = false } = {}) {
  const result = spawnSync('git', args, { encoding: 'utf8' })
  if (result.status !== 0) {
    if (allowFailure) return ''
    throw new Error(result.stderr || `git ${args.join(' ')} failed`)
  }
  return result.stdout
}

function readUntrackedFiles() {
  return parsePorcelainUntrackedFiles(runGit(['status', '--porcelain'], { allowFailure: true }))
}

function printWarnings(result) {
  for (const lock of result.expiredLocks) {
    console.warn(
      `agent-lock-check: expired lock ignored: ${lock.scope}:${lock.path} owner=${lock.owner} branch=${lock.branch} source=${lock.source || 'unknown'} expiresAt=${lock.expiresAt}`,
    )
  }
}

function printFailures(result, context) {
  if (result.duplicateLocks.length > 0) {
    console.error('agent-lock-check: duplicate active locks found')
    for (const duplicate of result.duplicateLocks) {
      console.error(`  ${duplicate.key}`)
      for (const lock of duplicate.locks) {
        console.error(`    owner=${lock.owner} branch=${lock.branch} task=${lock.task} source=${lock.source || 'unknown'}`)
      }
    }
  }

  if (result.blockingLocks.length > 0) {
    console.error('agent-lock-check: blocked by active lock')
    console.error(`  current owner=${context.agent || '(unknown)'} branch=${context.branch || '(unknown)'}`)
    for (const { file, lock } of result.blockingLocks) {
      console.error(`  file=${file}`)
      console.error(
        `    locked by owner=${lock.owner} branch=${lock.branch} task=${lock.task} source=${lock.source || 'unknown'} expiresAt=${lock.expiresAt}`,
      )
      console.error(`    reason=${lock.reason}`)
    }
  }

  if (result.missingHotPathLocks && result.missingHotPathLocks.length > 0) {
    console.error('agent-lock-check: high-risk path changed without active lock')
    console.error(`  current owner=${context.agent || '(unknown)'} branch=${context.branch || '(unknown)'}`)
    for (const { file, hotPath } of result.missingHotPathLocks) {
      console.error(`  file=${file}`)
      console.error(`    hot-path pattern=${hotPath.pattern}`)
      console.error(`    reason=${hotPath.reason}`)
      console.error(`    required: add an active lock covering this path to .agent-locks.yml`)
    }
  }

  if (result.selfMergeOrphans && result.selfMergeOrphans.length > 0) {
    console.error('agent-lock-check: PR would merge its own lock entry to main')
    console.error('  This creates an orphan on main as soon as the PR head branch is closed.')
    console.error('  Fix: release the lock BEFORE merging:')
    console.error('    npm run agent:lock-release -- --all')
    console.error('  Then push and re-run the check.')
    for (const lock of result.selfMergeOrphans) {
      console.error(
        `  ${lock.scope}:${lock.path} owner=${lock.owner} branch=${lock.branch} source=${lock.source || 'unknown'}`,
      )
    }
  }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  main()
}
