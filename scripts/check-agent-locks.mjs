#!/usr/bin/env node
// Input: .agent-locks.yml, current agent context, and a Git diff range
// Output: exits non-zero when current changes touch another Agent's active lock
// Pos: scripts/多 Agent 文件锁门禁
// 维护声明: 若锁字段、Agent 上下文格式或分支同步规则变化，请同步更新 AGENTS.md、RULES.md 和 scripts/README.md。
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const DEFAULT_LOCK_FILE = '.agent-locks.yml'

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

export function checkAgentLocks({ changedFiles, context, locks, now = new Date() }) {
  const duplicateLocks = findDuplicateActiveLocks(locks, now)
  const blockingLocks = findBlockingLocks({ changedFiles, context, locks, now })
  const expiredLocks = locks.filter((lock) => isExpiredLock(lock, now))

  return {
    ok: duplicateLocks.length === 0 && blockingLocks.length === 0,
    blockingLocks,
    duplicateLocks,
    expiredLocks,
  }
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

function isSameLogicalLock(left, right) {
  return left.owner === right.owner && left.branch === right.branch && left.task === right.task
}

function main() {
  const options = parseArgs(process.argv.slice(2))
  const rootDir = runGit(['rev-parse', '--show-toplevel']).trim()
  const lockFile = path.join(rootDir, options.lockFile)
  const localRegistry = fs.existsSync(lockFile)
    ? parseAgentLocks(fs.readFileSync(lockFile, 'utf8'))
    : { version: 1, locks: [] }
  const registrySources = [{ source: 'working-tree', registry: localRegistry }]
  if (options.includeRemoteLocks) {
    registrySources.push(...readRemoteLockRegistries(options.lockFile))
  }
  const context = loadContextFromWorktree(rootDir)
  const locks = collectLocksFromRegistries(registrySources, { currentBranch: context.branch })
  const changedFiles = options.changedFiles ?? readChangedFiles({ base: options.base, head: options.head })
  const result = checkAgentLocks({
    changedFiles,
    context,
    locks,
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
}

if (import.meta.url === `file://${process.argv[1]}`) {
  main()
}
