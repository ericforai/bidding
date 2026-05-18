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
const FAIL_ON_HEX = process.argv.includes('--fail-on-hex');
const MAX_HEX = parseInt(process.argv.find(arg => arg.startsWith('--max-hex='))?.split('=')[1] || '0');

// NEVER count variables.css as an offender — it defines the tokens
const IGNORE_FILES = ['src/styles/variables.css', 'src/styles/variables.scss'];

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
  const relPath = file.startsWith(ROOT) ? file.slice(ROOT.length + 1) : file;
  if (IGNORE_FILES.includes(relPath)) continue;

  const content = readFileSync(file, 'utf-8');
  const hexCount = countOccurrences(content, hexRe);
  const tokenCount = countOccurrences(content, tokenRe);
  
  if (hexCount > 0 || tokenCount > 0) {
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
console.log('\n' + '='.repeat(70));
console.log('  Design Token Coverage Report');
console.log('  ' + new Date().toISOString().slice(0, 10));
console.log('='.repeat(70));

console.log('\n  By Directory:');
console.log('  ' + '-'.repeat(66));
console.log(`  ${'Directory'.padEnd(25)} ${'Hardcoded'.padStart(12)} ${'Tokens'.padStart(12)} ${'Coverage'.padStart(12)}`);
console.log('  ' + '-'.repeat(66));
for (const [dir, { hexCount, tokenCount }] of Object.entries(byDir).sort()) {
  const pct = (hexCount + tokenCount) > 0
    ? ((tokenCount / (hexCount + tokenCount)) * 100).toFixed(1)
    : '0.0';
  console.log(`  ${dir.padEnd(25)} ${hexCount.toString().padStart(12)} ${tokenCount.toString().padStart(12)} ${pct.padStart(11)}%`);
}
console.log('  ' + '-'.repeat(66));
console.log(`  ${'TOTAL'.padEnd(25)} ${totalHex.toString().padStart(12)} ${totalToken.toString().padStart(12)} ${coveragePct.padStart(11)}%`);

console.log('\n  Top 15 files with most hardcoded colors:');
console.log('  ' + '-'.repeat(70));
for (const [file, { hexCount, tokenCount }] of offenders.slice(0, 15)) {
  const displayFile = file.length > 50 ? '...' + file.slice(-47) : file;
  console.log(`  ${hexCount.toString().padStart(4)}  ${displayFile.padEnd(52)} (tokens: ${tokenCount})`);
}

if (totalHex > 1000) {
  console.log(`\n  ⚠️  Still ${totalHex} hardcoded colors. Major debt detected.\n`);
} else if (totalHex === 0) {
  console.log('\n  ✅ No hardcoded colors found. Design token coverage complete!\n');
} else {
  console.log(`\n  👍 Down to ${totalHex} hardcoded colors. Keep migrating.\n`);
}

if (FAIL_ON_HEX && totalHex > MAX_HEX) {
  console.error(`\n  ❌ Error: ${totalHex} hardcoded colors found. Design token governance allows max ${MAX_HEX} hex colors.\n`);
  process.exit(1);
}

