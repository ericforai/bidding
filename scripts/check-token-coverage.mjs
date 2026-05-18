#!/usr/bin/env node
// Input: src/ directory (.css and .vue files)
// Output: token coverage report to stdout
// Pos: scripts/check-token-coverage.mjs - Design token coverage checker
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
import { execSync } from 'child_process';
import { readFileSync } from 'fs';
import { join } from 'path';

const ROOT = process.cwd();
const SRC = join(ROOT, 'src');

function findAllFiles(dir, ext) {
  try {
    const out = execSync(`find ${dir} -name '*.${ext}' -not -path '*/node_modules/*'`, {
      encoding: 'utf-8', cwd: ROOT,
    });
    return out.trim().split('\n').filter(Boolean);
  } catch { return []; }
}

function countOccurrences(content, regex) {
  const matches = content.match(regex);
  return matches ? matches.length : 0;
}

// Collect all files
const cssFiles = findAllFiles(SRC, 'css');
const vueFiles = findAllFiles(SRC, 'vue');
const allFiles = [...cssFiles, ...vueFiles];

// Analyze each file
const hexRe = /#[0-9a-fA-F]{3,6}\b/g;
const tokenRe = /var\(--/g;

const fileStats = {};

for (const file of allFiles) {
  const content = readFileSync(file, 'utf-8');
  const hexCount = countOccurrences(content, hexRe);
  const tokenCount = countOccurrences(content, tokenRe);
  if (hexCount > 0 || tokenCount > 0) {
    const relPath = file.startsWith(ROOT) ? file.slice(ROOT.length + 1) : file;
    fileStats[relPath] = { hexCount, tokenCount };
  }
}

// Aggregate by directory
const byDir = {};
let totalHex = 0, totalToken = 0;

for (const [file, { hexCount, tokenCount }] of Object.entries(fileStats)) {
  const dir = file.split('/').slice(0, 2).join('/');
  if (!byDir[dir]) byDir[dir] = { hexCount: 0, tokenCount: 0 };
  byDir[dir].hexCount += hexCount;
  byDir[dir].tokenCount += tokenCount;
  totalHex += hexCount;
  totalToken += tokenCount;
}

// Sort offenders
const offenders = Object.entries(fileStats)
  .sort((a, b) => b[1].hexCount - a[1].hexCount);

const total = totalHex + totalToken;
const coveragePct = total > 0 ? ((totalToken / total) * 100).toFixed(1) : '0.0';

// --- Report ---
console.log('\n' + '='.repeat(60));
console.log('  Design Token Coverage Report');
console.log('  ' + new Date().toISOString().slice(0, 10));
console.log('='.repeat(60));

console.log('\n  By Directory:');
console.log('  ' + '-'.repeat(56));
console.log('  %-22s %12s %12s %10s', 'Directory', 'Hardcoded', 'Tokens', 'Coverage');
console.log('  ' + '-'.repeat(56));
for (const [dir, { hexCount, tokenCount }] of Object.entries(byDir).sort()) {
  const pct = (hexCount + tokenCount) > 0
    ? ((tokenCount / (hexCount + tokenCount)) * 100).toFixed(1)
    : '0.0';
  console.log('  %-22s %12d %12d %9s%%', dir, hexCount, tokenCount, pct);
}
console.log('  ' + '-'.repeat(56));
console.log('  %-22s %12d %12d %9s%%', 'TOTAL', totalHex, totalToken, coveragePct);

console.log('\n  Top 15 files with most hardcoded colors:');
console.log('  ' + '-'.repeat(60));
for (const [file, { hexCount, tokenCount }] of offenders.slice(0, 15)) {
  console.log('  %4d  %-50s  (tokens: %d)', hexCount, file.length > 48 ? '...' + file.slice(-45) : file, tokenCount);
}

if (totalHex > 1000) {
  console.log(`\n  ⚠️  Still ${totalHex} hardcoded colors. More work needed.\n`);
} else if (totalHex === 0) {
  console.log('\n  ✅ No hardcoded colors found. Design token coverage complete!\n');
} else {
  console.log(`\n  👍 Down to ${totalHex} hardcoded colors. Keep migrating.\n`);
}
