import { describe, expect, it } from 'vitest'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { extname, join, relative } from 'node:path'

const repoRoot = process.cwd()
const guardedRoot = join(repoRoot, 'src/views/Bidding/list')
const guardedExtensions = new Set(['.vue', '.js', '.css'])

function collectFiles(dir) {
  return readdirSync(dir).flatMap((entry) => {
    const filePath = join(dir, entry)
    if (statSync(filePath).isDirectory()) {
      return collectFiles(filePath)
    }
    if (!guardedExtensions.has(extname(filePath)) || filePath.endsWith('.spec.js')) {
      return []
    }
    return [filePath]
  })
}

describe('Bidding list line budget', () => {
  it('keeps the page shell and local split files under 300 lines', () => {
    const files = [join(repoRoot, 'src/views/Bidding/List.vue'), ...collectFiles(guardedRoot)]
    const oversized = files
      .map((filePath) => ({
        file: relative(repoRoot, filePath),
        lines: readFileSync(filePath, 'utf-8').split('\n').length,
      }))
      .filter((item) => item.lines > 300)

    expect(oversized).toEqual([])
  })
})
