#!/usr/bin/env node
// Input: src/ directory (.css and .vue files)
// Output: modified files with hex colors replaced by CSS var() references
// Pos: scripts/migrate-colors.mjs - Color token migration script
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
import { readFileSync, writeFileSync } from 'fs';
import { execSync } from 'child_process';
import { join, relative } from 'path';

const ROOT = process.cwd();
const DRY_RUN = process.argv.includes('--dry-run');
const VERBOSE = process.argv.includes('--verbose');

// Build lookup: normalized hex → CSS variable
const COLOR_MAP = new Map([
  // Brand greens
  ['2e7659', 'var(--brand-xiyu-logo)'],
  ['27674e', 'var(--brand-xiyu-logo-hover)'],
  ['1f553f', 'var(--brand-xiyu-logo-active)'],
  ['e7f2ed', 'var(--brand-xiyu-logo-light)'],
  // Brand blues
  ['0066cc', 'var(--brand-primary)'],
  ['3388dd', 'var(--brand-primary-light)'],
  ['0052a3', 'var(--brand-primary-dark)'],
  // Functional
  ['00aa44', 'var(--color-success)'],
  ['ff8800', 'var(--color-warning)'],
  ['dd2200', 'var(--color-danger)'],
  // Grays
  ['f5f7fa', 'var(--gray-50)'],
  ['e8e8e8', 'var(--gray-100)'],
  ['e8ecf0', 'var(--gray-150)'],
  ['d0d0d0', 'var(--gray-200)'],
  ['e4e7ed', 'var(--gray-250)'],
  ['b0b0b0', 'var(--gray-300)'],
  ['909399', 'var(--gray-350)'],
  ['999999', 'var(--gray-400)'],
  ['666666', 'var(--gray-500)'],
  ['606266', 'var(--gray-550)'],
  ['444444', 'var(--gray-600)'],
  ['333333', 'var(--gray-700)'],
  ['303133', 'var(--gray-750)'],
  ['222222', 'var(--gray-800)'],
  ['1a1a1a', 'var(--gray-900)'],
  ['6b7280', 'var(--gray-650)'],
  ['111827', 'var(--gray-950)'],
  // Semantic text colors (preferred - last set wins)
  ['1a1a1a', 'var(--text-primary)'],
  ['666666', 'var(--text-secondary)'],
  ['606266', 'var(--text-secondary-ui)'],
  ['999999', 'var(--text-tertiary)'],
  ['909399', 'var(--text-muted)'],
  ['64748b', 'var(--text-slate)'],
  ['bbbbbb', 'var(--text-placeholder)'],
  // Sidebar
  ['334155', 'var(--sidebar-text)'],
  ['475569', 'var(--sidebar-text-secondary)'],
  // Backgrounds (preferred - last set wins)
  ['f5f7fa', 'var(--bg-subtle)'],
  ['ffffff', 'var(--bg-card)'],
  ['f0f2f5', 'var(--bg-page)'],
  // Accent
  ['0369a1', 'var(--accent-blue)'],
  ['e0f2fe', 'var(--accent-blue-light)'],
]);

// 3-hex shortcuts
const SHORT_MAP = {
  'fff': 'ffffff',
};

function normalizeHex(hex) {
  let h = hex.replace(/^#/, '').toLowerCase();
  // Expand 3-digit hex to 6-digit
  if (h.length === 3) {
    h = h[0] + h[0] + h[1] + h[1] + h[2] + h[2];
  }
  return h;
}

function findFiles(dir, ext) {
  try {
    const out = execSync(`find ${dir} -name '*.${ext}' -not -path '*/node_modules/*' 2>/dev/null`, {
      encoding: 'utf-8', cwd: ROOT,
    });
    return out.trim().split('\n').filter(Boolean);
  } catch { return []; }
}

// NEVER modify variables.css — it defines the tokens, don't self-reference them
const SKIP_FILES = ['src/styles/variables.css'];

function fileIncluded(fullPath) {
  const rel = relative(ROOT, fullPath);
  return !SKIP_FILES.includes(rel);
}

const cssFiles = findFiles(join(ROOT, 'src/styles'), 'css').filter(fileIncluded);
cssFiles.push(...findFiles(join(ROOT, 'src/views/Dashboard/styles'), 'css').filter(fileIncluded));
cssFiles.push(...findFiles(join(ROOT, 'src/views/Bidding'), 'css').filter(fileIncluded));

const vueFiles = [
  'src/views/AI/components/FeatureCard.vue',
  'src/views/AI/Center.vue',
  'src/views/Project/List.vue',
  'src/components/layout/Header.vue',
  'src/components/layout/Sidebar.vue',
  'src/components/common/TaskBoard.vue',
  'src/components/common/NotificationPanel.vue',
  'src/components/login/LoginForm.vue',
].map(f => join(ROOT, f));

// AI components (ui-focused subdirectory)
const aiVueFiles = findFiles(join(ROOT, 'src/components/ai'), 'vue');
vueFiles.push(...aiVueFiles);

// Resource components
['MobileCard.vue', 'VersionControl.vue', 'SmartAssistantPanel.vue', 'CollaborationCenter.vue', 'ScoreCoverage.vue'].forEach(f => {
  const p = join(ROOT, 'src/components/ai', f);
  try { readFileSync(p); vueFiles.push(p); } catch {}
});

let totalReplaced = 0;
let totalFiles = 0;

// Helper: compute relative @import path to variables.css
function getImportPath(filePath) {
  const fileDir = filePath.substring(0, filePath.lastIndexOf('/'));
  const stylesDir = join(ROOT, 'src/styles');
  const rel = relative(fileDir, stylesDir);
  return (rel || '.') + '/variables.css';
}

function replaceColorsInContent(content) {
  let replaced = 0;

  const result = content.replace(/#[0-9a-fA-F]{3,6}\b/g, (match, offset) => {
    const normalized = normalizeHex(match);
    // Don't replace colors inside existing var() calls
    const before = content.substring(Math.max(0, offset - 20), offset);
    if (/var\([^)]*$/.test(before)) {
      return match;
    }
    if (COLOR_MAP.has(normalized)) {
      replaced++;
      return COLOR_MAP.get(normalized);
    }
    return match;
  });

  return { content: result, replaced };
}

// Process CSS files
for (const file of cssFiles) {
  let content = readFileSync(file, 'utf-8');
  const relPath = relative(ROOT, file);

  // Add @import only for view-level CSS files (not src/styles/ — loaded by main.js)
  const isStyleFile = relPath.startsWith('src/styles/');
  let needsImport = !isStyleFile && !content.includes('variables.css');
  let importLine = '';
  if (needsImport) {
    importLine = `@import '${getImportPath(file)}';\n\n`;
  }

  const { content: newContent, replaced } = replaceColorsInContent(content);

  if (replaced > 0 || needsImport) {
    totalFiles++;
    totalReplaced += replaced;
    const finalContent = needsImport ? importLine + newContent : newContent;
    if (!DRY_RUN) {
      writeFileSync(file, finalContent, 'utf-8');
    }
    console.log(`  ${DRY_RUN ? '[DRY]' : '[OK]'}  ${relPath}  → ${replaced} replaces${needsImport ? ' (+@import)' : ''}`);
  }
}

// Process Vue SFC files
for (const file of vueFiles) {
  try { readFileSync(file, 'utf-8'); } catch { continue; }
  let content = readFileSync(file, 'utf-8');
  const relPath = relative(ROOT, file);

  const { content: newContent, replaced } = replaceColorsInContent(content);

  if (replaced > 0) {
    totalFiles++;
    totalReplaced += replaced;
    if (!DRY_RUN) {
      writeFileSync(file, newContent, 'utf-8');
    }
    console.log(`  ${DRY_RUN ? '[DRY]' : '[OK]'}  ${relPath}  → ${replaced} replaces`);
  }
}

console.log(`\nDone: ${totalFiles} files modified, ${totalReplaced} colors replaced.`);
