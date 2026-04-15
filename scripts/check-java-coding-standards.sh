#!/usr/bin/env bash
# Input: staged Java files from git index and repository root state
# Output: pre-commit validation result for Java coding standards and compile gate
# Pos: scripts/质量守卫脚本
# 维护声明: 仅维护 Java 提交门禁；规则变更请同步团队编码规范与 hooks 文档。
set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel)"
cd "$ROOT_DIR"

STAGED_JAVA_FILES=()
while IFS= read -r file; do
  if [ -n "$file" ]; then
    STAGED_JAVA_FILES+=("$file")
  fi
done <<EOF
$(git diff --cached --name-only --diff-filter=ACMR | grep -E '^backend/src/main/java/.*\.java$' || true)
EOF

if [ "${#STAGED_JAVA_FILES[@]}" -eq 0 ]; then
  echo "java-standards: no staged Java files, skip."
  exit 0
fi

QUALITY_INCLUDE_PATTERNS=()
QUALITY_ANALYZE_CLASSES=()
for file in "${STAGED_JAVA_FILES[@]}"; do
  relative_source="${file#backend/src/main/java/}"
  if [ "$relative_source" = "$file" ]; then
    continue
  fi
  QUALITY_INCLUDE_PATTERNS+=("$relative_source")
  QUALITY_ANALYZE_CLASSES+=("${relative_source%.java}")
done

QUALITY_INCLUDES=""
if [ "${#QUALITY_INCLUDE_PATTERNS[@]}" -gt 0 ]; then
  QUALITY_INCLUDES="$(IFS=,; echo "${QUALITY_INCLUDE_PATTERNS[*]}")"
fi

QUALITY_ONLY_ANALYZE=""
if [ "${#QUALITY_ANALYZE_CLASSES[@]}" -gt 0 ]; then
  for i in "${!QUALITY_ANALYZE_CLASSES[@]}"; do
    QUALITY_ANALYZE_CLASSES[$i]="${QUALITY_ANALYZE_CLASSES[$i]//\//.}"
  done
  QUALITY_ONLY_ANALYZE="$(IFS=,; echo "${QUALITY_ANALYZE_CLASSES[*]}")"
fi

echo "java-standards: checking ${#STAGED_JAVA_FILES[@]} staged Java file(s)..."

HAS_ERROR=0

for file in "${STAGED_JAVA_FILES[@]}"; do
  staged_patch="$(git diff --cached -U0 -- "$file" | grep -E '^\+' | grep -vE '^\+\+\+' || true)"

  if [ -z "$staged_patch" ]; then
    continue
  fi

  if printf "%s\n" "$staged_patch" | grep -nE 'catch[[:space:]]*\([[:space:]]*Exception[[:space:]]+[A-Za-z_][A-Za-z0-9_]*[[:space:]]*\)' >/tmp/java_hook_match.txt; then
    echo "ERROR: avoid broad catch(Exception) in $file"
    sed 's/^/  line /' /tmp/java_hook_match.txt
    HAS_ERROR=1
  fi

  if printf "%s\n" "$staged_patch" | grep -nE 'Optional[^;]*\.get[[:space:]]*\(' >/tmp/java_hook_match.txt; then
    echo "ERROR: avoid Optional.get() in $file; use map/flatMap/orElseThrow."
    sed 's/^/  line /' /tmp/java_hook_match.txt
    HAS_ERROR=1
  fi

  if printf "%s\n" "$staged_patch" | grep -nE 'throw[[:space:]]+new[[:space:]]+IllegalArgumentException[[:space:]]*\(' >/tmp/java_hook_match.txt; then
    echo "WARN: consider domain-specific exception instead of IllegalArgumentException in $file"
    sed 's/^/  line /' /tmp/java_hook_match.txt
  fi

  if printf "%s\n" "$staged_patch" | grep -nE 'import[[:space:]]+.+\.\*[[:space:]]*;' >/tmp/java_hook_match.txt; then
    echo "ERROR: avoid wildcard import in $file"
    sed 's/^/  line /' /tmp/java_hook_match.txt
    HAS_ERROR=1
  fi

  if printf "%s\n" "$staged_patch" | grep -nE 'System\.(out|err)\.print' >/tmp/java_hook_match.txt; then
    echo "ERROR: avoid System.out/err print in $file; use logger."
    sed 's/^/  line /' /tmp/java_hook_match.txt
    HAS_ERROR=1
  fi

  if printf "%s\n" "$staged_patch" | grep -nE '\b(List|Set|Map|Optional)\s+[A-Za-z_][A-Za-z0-9_]*\s*[=;,\)]' >/tmp/java_hook_match.txt; then
    echo "ERROR: avoid raw generic types in $file"
    sed 's/^/  line /' /tmp/java_hook_match.txt
    HAS_ERROR=1
  fi
done

if [ "$HAS_ERROR" -ne 0 ]; then
  echo
  echo "java-standards: blocked by rule violations."
  exit 1
fi

SPOTBUGS_MODE="${JAVA_STANDARDS_SPOTBUGS:-auto}"
ENABLE_SPOTBUGS=0
PMD_MODE="${JAVA_STANDARDS_PMD:-off}"
ENABLE_PMD=0

if [ "$SPOTBUGS_MODE" = "on" ]; then
  ENABLE_SPOTBUGS=1
elif [ "$SPOTBUGS_MODE" = "off" ]; then
  ENABLE_SPOTBUGS=0
elif [ "$SPOTBUGS_MODE" = "auto" ]; then
  if command -v curl >/dev/null 2>&1 && curl -fsSIL --connect-timeout 3 --max-time 8 "https://repo.maven.apache.org/maven2/" >/dev/null 2>&1; then
    ENABLE_SPOTBUGS=1
  fi
else
  echo "ERROR: invalid JAVA_STANDARDS_SPOTBUGS value: $SPOTBUGS_MODE (use auto|on|off)"
  exit 1
fi

if [ "$PMD_MODE" = "on" ]; then
  ENABLE_PMD=1
elif [ "$PMD_MODE" = "off" ]; then
  ENABLE_PMD=0
elif [ "$PMD_MODE" = "auto" ]; then
  ENABLE_PMD=0
else
  echo "ERROR: invalid JAVA_STANDARDS_PMD value: $PMD_MODE (use auto|on|off)"
  exit 1
fi

MAVEN_PROFILES="java-quality"
run_checkstyle_gate() {
  local profiles="$1"
  (cd backend && mvn -q \
    -Dmaven.test.skip=true \
    -Djacoco.skip=true \
    -P"$profiles" \
    -Dquality.skip=false \
    -Dquality.includes="$QUALITY_INCLUDES" \
    -Dquality.onlyAnalyze="$QUALITY_ONLY_ANALYZE" \
    checkstyle:check)
}

run_pmd_gate() {
  local profiles="$1"
  (cd backend && mvn -q \
    -Dmaven.test.skip=true \
    -Djacoco.skip=true \
    -P"$profiles" \
    -Dquality.skip=false \
    -Dquality.includes="$QUALITY_INCLUDES" \
    -Dquality.onlyAnalyze="$QUALITY_ONLY_ANALYZE" \
    pmd:check)
}

if [ "$ENABLE_SPOTBUGS" -eq 1 ]; then
  echo "java-standards: spotbugs enabled (mode=$SPOTBUGS_MODE)."
else
  echo "java-standards: spotbugs disabled (mode=$SPOTBUGS_MODE)."
fi

echo "java-standards: running checkstyle gate..."
run_checkstyle_gate "java-quality"

if [ "$ENABLE_PMD" -eq 1 ]; then
  echo "java-standards: PMD enabled (mode=$PMD_MODE)."
  echo "java-standards: running PMD gate..."
  run_pmd_gate "java-quality"
else
  echo "java-standards: PMD disabled (mode=$PMD_MODE)."
fi

if [ "$ENABLE_SPOTBUGS" -eq 1 ]; then
  echo "java-standards: running spotbugs gate..."
  if ! (cd backend && mvn -q \
    -Dmaven.test.skip=true \
    -Djacoco.skip=true \
    -P"java-quality,java-quality-spotbugs" \
    -Dquality.skip=false \
    -Dquality.includes="$QUALITY_INCLUDES" \
    -Dquality.onlyAnalyze="$QUALITY_ONLY_ANALYZE" \
    spotbugs:check); then
    if [ "$SPOTBUGS_MODE" = "auto" ]; then
      echo "java-standards: spotbugs failed in auto mode, skip spotbugs."
    else
      exit 1
    fi
  fi
fi

echo "java-standards: passed."
