#!/usr/bin/env node
// Input: .agent-locks.yml, git branch state, and current timestamp
// Output: removes stale expired locks (>24h past expiresAt with no recent commits)
// Pos: scripts/Agent 锁清理工具
// 维护声明: 若锁过期策略或清理阈值变化，请同步更新 RULES.md 和 scripts/README.md。

import fs from 'node:fs'
import path from 'path'
import { spawnSync } from 'node:child_process'

const DEFAULT_LOCK_FILE = '.agent-locks.yml'
const STALE_THRESHOLD_HOURS = 24

function main() {
  const options = parseArgs(process.argv.slice(2))
  const rootDir = runGit(['rev-parse', '--show-toplevel']).trim()
  const lockFile = path.join(rootDir, options.lockFile)

  if (!fs.existsSync(lockFile)) {
    console.log('agent-locks-prune: no .agent-locks.yml found, nothing to prune')
    process.exit(0)
  }

  const content = fs.readFileSync(lockFile, 'utf8')
  const lines = content.split(/\r?\n/)
  const now = new Date()
  const staleThreshold = now.getTime() - STALE_THRESHOLD_HOURS * 60 * 60 * 1000

  const remoteBranches = getRemoteBranches()
  const locks = parseLocks(lines)
  const staleLocks = locks.filter((lock) => isStale(lock, staleThreshold, remoteBranches))

  if (staleLocks.length === 0) {
    console.log('agent-locks-prune: no stale locks found')
    process.exit(0)
  }

  const prunedLines = removeStaleLocksFromLines(lines, staleLocks)
  fs.writeFileSync(lockFile, prunedLines.join('\n'), 'utf8')

  console.log(`agent-locks-prune: removed ${staleLocks.length} stale lock(s)`)
  for (const lock of staleLocks) {
    console.log(`  ${lock.scope}:${lock.path} owner=${lock.owner} branch=${lock.branch} expiresAt=${lock.expiresAt}`)
  }
}

function parseLocks(lines) {
  const locks = []
  let currentLock = null
  let startLine = -1

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const trimmed = line.trim()

    if (trimmed.startsWith('- ')) {
      if (currentLock) {
        currentLock.endLine = i - 1
      }
      currentLock = { startLine: i }
      locks.push(currentLock)
      assignField(currentLock, trimmed.slice(2))
      continue
    }

    if (line.startsWith('    ') && currentLock) {
      assignField(currentLock, trimmed)
    }
  }

  if (currentLock) {
    currentLock.endLine = lines.length - 1
  }

  return locks
}

function assignField(target, fieldLine) {
  const separatorIndex = fieldLine.indexOf(':')
  if (separatorIndex === -1) return
  const key = fieldLine.slice(0, separatorIndex).trim()
  const value = fieldLine.slice(separatorIndex + 1).trim().replace(/^["']|["']$/g, '')
  target[key] = value
}

function isStale(lock, staleThreshold, remoteBranches) {
  // Orphan-branch detection: if the lock's branch no longer exists on origin
  // (and isn't a long-lived branch like main/master), the lock is by definition
  // orphaned regardless of expiresAt. This catches the common pattern where a
  // PR self-locks for hot-path enforcement and the squash-merge preserves the
  // lock entry on main even though the branch is deleted.
  const longLived = new Set(['main', 'master'])
  if (lock.branch && !longLived.has(lock.branch) && !remoteBranches.includes(lock.branch)) {
    return true
  }

  if (!lock.expiresAt) return false

  const expiresAtTime = new Date(lock.expiresAt).getTime()
  if (expiresAtTime > staleThreshold) return false

  // Expired >24h. Check if branch still exists remotely.
  if (lock.branch && remoteBranches.includes(lock.branch)) {
    // Branch exists. Check if it has commits in the past 24h.
    const recentCommits = runGit(
      ['log', '--oneline', '--since=24 hours ago', `origin/${lock.branch}`],
      { allowFailure: true },
    ).trim()
    if (recentCommits) {
      // Branch is active, don't prune even if lock expired
      return false
    }
  }

  return true
}

function removeStaleLocksFromLines(lines, staleLocks) {
  const staleRanges = staleLocks.map((lock) => ({ start: lock.startLine, end: lock.endLine }))
  return lines.filter((_, i) => !staleRanges.some((range) => i >= range.start && i <= range.end))
}

function getRemoteBranches() {
  const output = runGit(['branch', '-r', '--format=%(refname:short)'], { allowFailure: true })
  return output
    .split(/\r?\n/)
    .map((line) => line.trim().replace(/^origin\//, ''))
    .filter(Boolean)
}

function parseArgs(args) {
  const options = {
    lockFile: DEFAULT_LOCK_FILE,
  }

  for (let i = 0; i < args.length; i++) {
    const arg = args[i]
    if (arg === '--lock-file') {
      options.lockFile = args[++i]
    }
  }

  return options
}

function runGit(args, { allowFailure = false } = {}) {
  const result = spawnSync('git', args, { encoding: 'utf8' })
  if (result.status !== 0) {
    if (allowFailure) return ''
    throw new Error(result.stderr || `git ${args.join(' ')} failed`)
  }
  return result.stdout
}

main()
