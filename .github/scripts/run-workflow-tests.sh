#!/bin/bash
# 工作流测试脚本

set -o pipefail

# 支持通过环境变量配置 DOCKER_HOST，优先检测 Podman socket
if [ -z "$DOCKER_HOST" ]; then
    PODMAN_SOCK="$HOME/.local/share/containers/podman/machine/podman.sock"
    if [ -S "$PODMAN_SOCK" ]; then
        export DOCKER_HOST="unix://$PODMAN_SOCK"
    fi
fi

# 切换到仓库根目录
cd "$(git rev-parse --show-toplevel)"

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║       GitHub Workflows 本地测试 (act + podman)               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "开始时间: $(date)"
echo ""

TOTAL=0

# 根据事件类型和工作流选择事件文件
get_event_file() {
    local event=$1
    local workflow=$2
    case "$event" in
        push)
            # 对于 ci.yml、release.yml 等 branch push 工作流使用 branch_push.json
            # 对于 github-release.yml 等 tag push 工作流使用 tag_push.json
            if [[ "$workflow" == "github-release.yml" ]]; then
                echo ".github/events/tag_push.json"
            else
                echo ".github/events/branch_push.json"
            fi
            ;;
        pull_request)
            echo ".github/events/pull_request.json"
            ;;
        workflow_dispatch)
            # workflow_dispatch 使用默认事件或空事件
            echo ""
            ;;
        workflow_run)
            echo ".github/events/workflow_run.json"
            ;;
        release)
            echo ".github/events/release.json"
            ;;
        *)
            echo ".github/act-event.json"
            ;;
    esac
}

# 测试函数
run_test() {
    local name=$1
    local workflow=$2
    local event=$3
    local job=$4

    echo ">>> 测试: $name"
    local start=$(date +%s)

    # 获取对应事件的事件文件
    local event_file=$(get_event_file "$event")
    local event_args=""
    if [ -n "$event_file" ] && [ -f "$event_file" ]; then
        event_args="-e $event_file"
    fi

    if [ -n "$job" ]; then
        act "$event" -W ".github/workflows/$workflow" $event_args -j "$job" 2>&1 | tail -3
        local exit_code=$?
    else
        act "$event" -W ".github/workflows/$workflow" $event_args 2>&1 | tail -3
        local exit_code=$?
    fi

    local end=$(date +%s)
    local dur=$((end - start))
    TOTAL=$((TOTAL + dur))

    if [ $exit_code -eq 0 ]; then
        echo "✅ $name: ${dur}s (通过)"
    else
        echo "❌ $name: ${dur}s (失败, exit=$exit_code)"
    fi
    echo ""

    # 保存结果
    echo "$name,$dur,$exit_code" >> /tmp/workflow-results.csv
}

# 清空结果文件
echo "workflow,duration,exit_code" > /tmp/workflow-results.csv

# 运行测试
run_test "ci.yml" "ci.yml" "push" ""
run_test "docs.yml" "docs.yml" "workflow_dispatch" "build"
run_test "nightly.yml" "nightly.yml" "workflow_dispatch" "full-tests"
run_test "build-policy-api.yml" "build-policy-api.yml" "workflow_dispatch" "build-native"
run_test "e2e-tests.yml" "e2e-tests.yml" "workflow_dispatch" "e2e"

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                       测试汇总报告                            ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "结束时间: $(date)"
echo ""
echo "测试结果:"
cat /tmp/workflow-results.csv | column -t -s,
echo ""
echo "跳过的工作流 (5个):"
echo "  - _reusable-build.yml, _reusable-policy-api-build.yml (可复用)"
echo "  - release-drafter.yml, github-release.yml (需要特定事件)"
echo "  - release.yml (需要 secrets)"
echo ""
echo "⏱️ 总执行时间: ${TOTAL} 秒 ($((TOTAL / 60)) 分 $((TOTAL % 60)) 秒)"
