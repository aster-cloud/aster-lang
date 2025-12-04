#!/bin/bash
# 全量工作流本地测试脚本
# 使用 .actrc 配置

set -e

# 切换到仓库根目录
cd "$(git rev-parse --show-toplevel)"

# 支持通过环境变量配置 DOCKER_HOST，优先检测 Podman socket
if [ -z "$DOCKER_HOST" ]; then
    PODMAN_SOCK="$HOME/.local/share/containers/podman/machine/podman.sock"
    if [ -S "$PODMAN_SOCK" ]; then
        export DOCKER_HOST="unix://$PODMAN_SOCK"
    fi
fi

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 结果存储
declare -A RESULTS
declare -A TIMES
TOTAL_TIME=0
PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_fail() { echo -e "${RED}[FAIL]${NC} $1"; }
log_skip() { echo -e "${YELLOW}[SKIP]${NC} $1"; }

# 测试单个工作流
test_workflow() {
    local name="$1"
    local workflow="$2"
    local event="$3"
    local job="$4"
    local timeout="${5:-300}"

    echo ""
    echo "========================================="
    log_info "Testing: $name"
    echo "  Workflow: $workflow"
    echo "  Event: $event"
    [ -n "$job" ] && echo "  Job: $job"
    echo "========================================="

    START=$(date +%s)

    local cmd="act $event -W .github/workflows/$workflow"
    [ -n "$job" ] && cmd="$cmd -j $job"

    # 运行测试，超时控制
    if timeout "$timeout" bash -c "$cmd" 2>&1; then
        EXIT_CODE=0
    else
        EXIT_CODE=$?
    fi

    END=$(date +%s)
    DURATION=$((END - START))
    TOTAL_TIME=$((TOTAL_TIME + DURATION))
    TIMES["$name"]=$DURATION

    if [ $EXIT_CODE -eq 0 ]; then
        RESULTS["$name"]="PASS"
        PASS_COUNT=$((PASS_COUNT + 1))
        log_pass "$name completed in ${DURATION}s"
    elif [ $EXIT_CODE -eq 124 ]; then
        RESULTS["$name"]="TIMEOUT"
        FAIL_COUNT=$((FAIL_COUNT + 1))
        log_fail "$name timed out after ${timeout}s"
    else
        RESULTS["$name"]="FAIL"
        FAIL_COUNT=$((FAIL_COUNT + 1))
        log_fail "$name failed (exit: $EXIT_CODE) in ${DURATION}s"
    fi
}

# 跳过工作流
skip_workflow() {
    local name="$1"
    local reason="$2"
    RESULTS["$name"]="SKIP"
    TIMES["$name"]=0
    SKIP_COUNT=$((SKIP_COUNT + 1))
    log_skip "$name - $reason"
}

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║          GitHub Workflows Local Test Suite                   ║"
echo "║          Using act with .actrc configuration                 ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "Start time: $(date)"
echo ""

# ==================== 可测试的工作流 ====================

# 1. ci.yml - 主 CI 工作流 (push 事件)
test_workflow "ci.yml" "ci.yml" "push" "" 600

# 2. docs.yml - 文档构建 (workflow_dispatch)
test_workflow "docs.yml" "docs.yml" "workflow_dispatch" "build" 300

# 3. nightly.yml - 夜间构建 (workflow_dispatch, 仅 full-tests)
test_workflow "nightly.yml" "nightly.yml" "workflow_dispatch" "full-tests" 600

# 4. build-policy-api.yml - Policy API 构建 (workflow_dispatch)
test_workflow "build-policy-api.yml" "build-policy-api.yml" "workflow_dispatch" "build-native" 600

# 6. e2e-tests.yml - 端到端测试 (workflow_dispatch)
test_workflow "e2e-tests.yml" "e2e-tests.yml" "workflow_dispatch" "e2e" 600

# ==================== 跳过的工作流 ====================

# 可复用工作流 (被其他工作流调用)
skip_workflow "_reusable-build.yml" "Reusable workflow (called by ci.yml)"
skip_workflow "_reusable-policy-api-build.yml" "Reusable workflow (called by build-policy-api.yml)"

# 需要特定 GitHub 事件/secrets
skip_workflow "release-drafter.yml" "Requires pull_request/push events with GitHub context"
skip_workflow "github-release.yml" "Requires tag push or release event"
skip_workflow "release.yml" "Requires push to main with changesets"

# ==================== 汇总报告 ====================

echo ""
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                    TEST SUMMARY REPORT                        ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "End time: $(date)"
echo ""

printf "%-35s %10s %10s\n" "Workflow" "Time (s)" "Status"
echo "─────────────────────────────────────────────────────────────"

for name in "ci.yml" "docs.yml" "nightly.yml" "build-policy-api.yml" "e2e-tests.yml" \
            "_reusable-build.yml" "_reusable-policy-api-build.yml" \
            "release-drafter.yml" "github-release.yml" "release.yml"; do
    time="${TIMES[$name]:-0}"
    status="${RESULTS[$name]:-UNKNOWN}"

    case $status in
        PASS)    status_icon="✅ PASS" ;;
        FAIL)    status_icon="❌ FAIL" ;;
        TIMEOUT) status_icon="⏱️ TIMEOUT" ;;
        SKIP)    status_icon="⏭️ SKIP" ;;
        *)       status_icon="❓ $status" ;;
    esac

    printf "%-35s %10s %10s\n" "$name" "$time" "$status_icon"
done

echo "─────────────────────────────────────────────────────────────"
echo ""
echo "📊 Statistics:"
echo "   ✅ Passed:  $PASS_COUNT"
echo "   ❌ Failed:  $FAIL_COUNT"
echo "   ⏭️ Skipped: $SKIP_COUNT"
echo ""
echo "⏱️  Total Execution Time: ${TOTAL_TIME} seconds ($((TOTAL_TIME / 60)) min $((TOTAL_TIME % 60)) sec)"
echo ""

# 返回失败数作为退出码
exit $FAIL_COUNT
