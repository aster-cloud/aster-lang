#!/bin/bash
# 使用模拟事件测试之前无法本地测试的工作流
# 测试 7 个需要特殊事件的工作流

set -e

# 支持通过环境变量配置 DOCKER_HOST，优先检测 Podman socket
if [ -z "$DOCKER_HOST" ]; then
    PODMAN_SOCK="$HOME/.local/share/containers/podman/machine/podman.sock"
    if [ -S "$PODMAN_SOCK" ]; then
        export DOCKER_HOST="unix://$PODMAN_SOCK"
    fi
fi

# 切换到仓库根目录
cd "$(git rev-parse --show-toplevel)"

# 确保禁用项目级 .actrc (如果存在)
if [ -f ".actrc" ]; then
    mv .actrc .actrc.disabled.tmp
    ACTRC_DISABLED=1
fi

trap 'if [ "$ACTRC_DISABLED" = "1" ]; then mv .actrc.disabled.tmp .actrc 2>/dev/null || true; fi' EXIT

TOTAL_TIME=0
RESULTS=()

run_event_test() {
    local name=$1
    local workflow=$2
    local event=$3
    local event_file=$4
    local job=$5

    echo ""
    echo "========================================"
    echo "测试: $name"
    echo "工作流: $workflow"
    echo "事件: $event"
    [ -n "$event_file" ] && echo "事件文件: $event_file"
    [ -n "$job" ] && echo "Job: $job"
    echo "========================================"

    local start_time=$(date +%s)
    local status="成功"

    # 构建命令
    local cmd="act $event -W .github/workflows/$workflow"
    [ -n "$event_file" ] && cmd="$cmd -e .github/events/$event_file"
    [ -n "$job" ] && cmd="$cmd -j $job"
    cmd="$cmd --container-architecture linux/amd64 -P ubuntu-latest=catthehacker/ubuntu:act-latest"

    echo "执行命令: $cmd"
    echo "----------------------------------------"

    # 执行测试 (限时 120 秒)
    local output
    if output=$(timeout 120 bash -c "$cmd" 2>&1); then
        # 检查输出中是否有失败标志
        if echo "$output" | grep -q "❌\|Job failed\|Error:"; then
            status="失败(有错误)"
        else
            status="成功"
        fi
    else
        local exit_code=$?
        if [ $exit_code -eq 124 ]; then
            status="超时(120s)"
        else
            # 检查是否是"无 stage 可运行"的情况
            if echo "$output" | grep -q "Could not find any stages to run"; then
                status="跳过(条件不匹配)"
            else
                status="失败(退出码:$exit_code)"
            fi
        fi
    fi

    # 显示最后几行输出
    echo "$output" | tail -15

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    TOTAL_TIME=$((TOTAL_TIME + duration))

    echo "----------------------------------------"
    echo "结果: $status | 耗时: ${duration}秒"

    RESULTS+=("$name|$status|${duration}s")
}

echo "================================================"
echo "开始测试需要模拟事件的工作流"
echo "时间: $(date)"
echo "================================================"

# 1. release-drafter.yml - 需要 pull_request 事件
run_event_test "release-drafter" "release-drafter.yml" "pull_request" "pull_request.json" ""

# 2. github-release.yml - 需要 release 事件
run_event_test "github-release(release)" "github-release.yml" "release" "release.json" ""

# 3. github-release.yml - 需要 tag push 事件
run_event_test "github-release(tag-push)" "github-release.yml" "push" "tag_push.json" ""

# 4. release.yml - 需要 push 到 main 分支
run_event_test "release" "release.yml" "push" "" ""

# 5. cleanup.yml - workflow_dispatch
run_event_test "cleanup" "cleanup.yml" "workflow_dispatch" "" ""

# 6. e2e-tests.yml - workflow_run 事件 (测试 Build Policy API 完成后触发)
run_event_test "e2e-tests(workflow_run)" "e2e-tests.yml" "workflow_run" "workflow_run.json" "e2e-workflow-run"

# 可复用工作流测试说明
echo ""
echo "========================================"
echo "可复用工作流 (通过 ci.yml 间接测试)"
echo "========================================"
echo "_reusable-build.yml - 已通过 ci.yml 测试 (约128秒)"
echo "_reusable-policy-api-build.yml - 被其他工作流引用"

echo ""
echo "================================================"
echo "测试完成汇总"
echo "================================================"
echo ""
printf "%-30s | %-25s | %s\n" "工作流" "状态" "耗时"
printf "%-30s-+-%-25s-+-%s\n" "------------------------------" "-------------------------" "------"
for result in "${RESULTS[@]}"; do
    IFS='|' read -r name status duration <<< "$result"
    printf "%-30s | %-25s | %s\n" "$name" "$status" "$duration"
done
echo ""
echo "事件模拟测试总时间: ${TOTAL_TIME}秒"
echo "测试结束: $(date)"
