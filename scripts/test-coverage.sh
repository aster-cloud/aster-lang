#!/usr/bin/env bash
# CI 测试覆盖率验证脚本
# 用途：自动化运行测试并验证覆盖率阈值（≥80% line coverage, ≥75% branch coverage）

set -e  # 遇到错误立即退出
set -o pipefail  # 管道命令任一失败则整体失败

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Phase 1: 测试覆盖率验证${NC}"
echo -e "${YELLOW}========================================${NC}"

# 1. 清理旧的构建产物
echo -e "\n${GREEN}[1/5] 清理旧的构建产物...${NC}"
./gradlew clean --no-configuration-cache

# 2. 编译所有核心模块
echo -e "\n${GREEN}[2/5] 编译所有核心模块...${NC}"
./gradlew :aster-core:build \
          :aster-runtime:build \
          :aster-finance:build \
          :aster-policy-common:build \
          --no-configuration-cache \
          --parallel

# 3. 运行所有测试并生成聚合覆盖率报告
echo -e "\n${GREEN}[3/5] 运行测试并生成聚合覆盖率报告...${NC}"
./gradlew jacocoAggregateReport --no-configuration-cache

# 4. 验证覆盖率阈值
echo -e "\n${GREEN}[4/5] 验证覆盖率阈值 (≥80% line, ≥75% branch)...${NC}"
if ./gradlew jacocoAggregateVerification --no-configuration-cache; then
    echo -e "${GREEN}✓ 覆盖率验证通过${NC}"
else
    echo -e "${RED}✗ 覆盖率验证失败：未达到阈值要求${NC}"
    echo -e "${YELLOW}请查看报告：build/reports/jacoco/aggregate/html/index.html${NC}"
    exit 1
fi

# 5. 显示覆盖率摘要
echo -e "\n${GREEN}[5/5] 覆盖率摘要：${NC}"
REPORT_FILE="build/reports/jacoco/aggregate/html/index.html"
if [ -f "$REPORT_FILE" ]; then
    # 提取总体覆盖率（指令覆盖率）
    COVERAGE=$(grep -A 5 "Total" "$REPORT_FILE" | grep -oP '>\K[0-9]+%' | head -1 || echo "N/A")
    echo -e "${GREEN}  总体覆盖率: $COVERAGE${NC}"
    echo -e "${YELLOW}  详细报告: file://$PWD/$REPORT_FILE${NC}"
else
    echo -e "${RED}  未找到覆盖率报告文件${NC}"
    exit 1
fi

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}✓ 测试覆盖率验证完成${NC}"
echo -e "${GREEN}========================================${NC}"
