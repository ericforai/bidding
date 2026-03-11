#!/bin/bash
# 文档自洽性检查脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

# Input: 项目路径参数
# Output: Markdown 格式的检查报告
# Pos: 项目根目录 scripts/

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目路径
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_SRC="$PROJECT_ROOT/backend/src/main/java/com/xiyu/bid"
FRONTEND_SRC="$PROJECT_ROOT/src"

# 报告文件
REPORT_FILE="$PROJECT_ROOT/docs/doc-consistency-check-report.md"

# 白名单目录（无需 README）
WHITELIST="node_modules target dist build .git .worktrees .cache coverage assets"

# 计数器
backend_total=0
backend_has_readme=0
backend_compliant=0
frontend_total=0
frontend_has_readme=0
frontend_compliant=0

# 检查目录是否在白名单中
is_whitelisted() {
    local dir="$1"
    for item in $WHITELIST; do
        if [[ "$dir" == *"$item"* ]]; then
            return 0
        fi
    done
    return 1
}

# 检查 README 是否符合规范
check_readme_compliance() {
    local readme_path="$1"
    if [ ! -f "$readme_path" ]; then
        echo "❌ MISSING"
        return 1
    fi

    if grep -q "一旦我所属的文件夹有所变化" "$readme_path" 2>/dev/null; then
        echo "✅ COMPLIANT"
        return 0
    else
        echo "⚠️  NO_DECLARATION"
        return 2
    fi
}

# 开始生成报告
cat > "$REPORT_FILE" << EOF
# 文档自洽性检查报告

> 一旦我所属的文件夹有所变化，请更新我。

**生成时间**: $(date '+%Y-%m-%d %H:%M:%S')

EOF

echo -e "${YELLOW}开始检查...${NC}"
echo ""

# 检查后端模块
echo "## 后端模块检查" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 模块 | 状态 |" >> "$REPORT_FILE"
echo "|------|------|" >> "$REPORT_FILE"

for dir in "$BACKEND_SRC"/*/; do
    if [ -d "$dir" ]; then
        module=$(basename "$dir")
        ((backend_total++))

        status=$(check_readme_compliance "${dir}README.md")

        case "$status" in
            "✅ COMPLIANT")
                echo "| \`$module/\` | ✅ 符合规范 |" >> "$REPORT_FILE"
                ((backend_compliant++))
                ((backend_has_readme++))
                ;;
            "⚠️  NO_DECLARATION")
                echo "| \`$module/\` | ⚠️  缺少更新声明 |" >> "$REPORT_FILE"
                ((backend_has_readme++))
                ;;
            "❌ MISSING")
                echo "| \`$module/\` | ❌ 缺失 README |" >> "$REPORT_FILE"
                ;;
        esac
    fi
done

echo "" >> "$REPORT_FILE"

# 检查前端目录
echo "## 前端目录检查" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 目录 | 状态 |" >> "$REPORT_FILE"
echo "|------|------|" >> "$REPORT_FILE"

for dir_name in api components config router styles utils views; do
    dir="$FRONTEND_SRC/$dir_name"
    if [ -d "$dir" ]; then
        ((frontend_total++))

        status=$(check_readme_compliance "${dir}/README.md")

        case "$status" in
            "✅ COMPLIANT")
                echo "| \`src/$dir_name/\` | ✅ 符合规范 |" >> "$REPORT_FILE"
                ((frontend_compliant++))
                ((frontend_has_readme++))
                ;;
            "⚠️  NO_DECLARATION")
                echo "| \`src/$dir_name/\` | ⚠️  缺少更新声明 |" >> "$REPORT_FILE"
                ((frontend_has_readme++))
                ;;
            "❌ MISSING")
                echo "| \`src/$dir_name/\` | ❌ 缺失 README |" >> "$REPORT_FILE"
                ;;
        esac
    fi
done

echo "" >> "$REPORT_FILE"

# 生成摘要
echo "## 摘要" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 项目 | 总目录 | 有README | 符合规范 | 缺失README | 缺少声明 |" >> "$REPORT_FILE"
echo "|------|--------|----------|----------|------------|----------|" >> "$REPORT_FILE"
echo "| 后端 | $backend_total | $backend_has_readme | $backend_compliant | $((backend_total - backend_has_readme)) | $((backend_has_readme - backend_compliant)) |" >> "$REPORT_FILE"
echo "| 前端 | $frontend_total | $frontend_has_readme | $frontend_compliant | $((frontend_total - frontend_has_readme)) | $((frontend_has_readme - frontend_compliant)) |" >> "$REPORT_FILE"
echo "| **合计** | **$((backend_total + frontend_total))** | **$((backend_has_readme + frontend_has_readme))** | **$((backend_compliant + frontend_compliant))** | **$((backend_total + frontend_total - backend_has_readme - frontend_has_readme))** | **$((backend_has_readme + frontend_has_readme - backend_compliant - frontend_compliant))** |" >> "$REPORT_FILE"

# 检查结果
total_compliant=$((backend_compliant + frontend_compliant))
total_dirs=$((backend_total + frontend_total))

echo ""
echo -e "检查完成！"
echo ""
echo "后端: $backend_compliant/$backend_total 符合规范"
echo "前端: $frontend_compliant/$frontend_total 符合规范"
echo "总计: $total_compliant/$total_dirs 符合规范"
echo ""
echo "报告已保存至: $REPORT_FILE"

# 返回非零退出码表示有问题
if [ $total_compliant -lt $total_dirs ]; then
    exit 1
fi
