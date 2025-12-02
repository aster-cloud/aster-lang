#!/bin/bash
# Phase 3D Task 4.2: Native Image Golden Tests
# 验证 Native Image 与 JVM 模式输出一致性

set -e

NATIVE_ASTER="./aster-truffle/build/native/nativeCompile/aster"
JVM_JAR="./aster-truffle/build/libs/aster-truffle.jar"
GOLDEN_DIR="benchmarks/core"
PASSED=0
FAILED=0
TOTAL=0

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================="
echo "Native Image Golden Tests"
echo "=================================="
echo ""

# 检查 Native Image 二进制
if [ ! -f "$NATIVE_ASTER" ]; then
    echo -e "${RED}❌ Native Image not found: $NATIVE_ASTER${NC}"
    exit 1
fi

# 检查 JVM JAR
if [ ! -f "$JVM_JAR" ]; then
    echo -e "${RED}❌ JVM JAR not found: $JVM_JAR${NC}"
    exit 1
fi

# 检查测试目录
if [ ! -d "$GOLDEN_DIR" ]; then
    echo -e "${RED}❌ Test directory not found: $GOLDEN_DIR${NC}"
    exit 1
fi

echo -e "Native Image: ${GREEN}$NATIVE_ASTER${NC}"
echo -e "JVM JAR:      ${GREEN}$JVM_JAR${NC}"
echo -e "Test Dir:     ${GREEN}$GOLDEN_DIR${NC}"
echo ""
echo "Running tests..."
echo ""

# 遍历所有 Core IR JSON 文件
for test_file in "$GOLDEN_DIR"/*.json; do
    test_name=$(basename "$test_file")
    TOTAL=$((TOTAL + 1))

    echo -n "[${TOTAL}] Testing $test_name... "

    # 运行 Native Image 版本（只捕获最后一行非空输出）
    native_output=$("$NATIVE_ASTER" "$test_file" 2>&1)
    native_exit=$?
    native_result=$(echo "$native_output" | grep -v "^\[" | grep -v "^WARNING" | grep -v "^To disable" | grep -v "^Execution" | grep -v "^The following" | grep -v "^For more" | grep -v "^	at" | grep -v "^Original" | grep -v "^Exception" | grep -v "Suppressed:" | tail -1)

    if [ $native_exit -ne 0 ]; then
        # 如果退出码非零，检查是否有运行时异常
        if echo "$native_output" | grep -qE "(Exception in thread|Fatal Error:)"; then
            native_result="RUNTIME_ERROR"
        else
            echo -e "${RED}FAILED${NC} (Native Image execution failed)"
            FAILED=$((FAILED + 1))
            continue
        fi
    fi

    # 运行 JVM 版本（通过 Gradle run，只捕获最后一行非空输出）
    jvm_output=$(./gradlew :aster-truffle:run --args="$test_file" --quiet 2>&1)
    jvm_exit=$?
    jvm_result=$(echo "$jvm_output" | grep -v "^\[" | grep -v "^WARNING" | grep -v "^To disable" | grep -v "^Execution" | grep -v "^The following" | grep -v "^For more" | grep -v "^	at" | grep -v "^Original" | grep -v "^Exception" | grep -v "Suppressed:" | grep -v "^>" | grep -v "^Task" | tail -1)

    if [ $jvm_exit -ne 0 ]; then
        # 如果退出码非零，检查是否有运行时异常
        if echo "$jvm_output" | grep -qE "(Exception in thread|Fatal Error:)"; then
            jvm_result="RUNTIME_ERROR"
        else
            echo -e "${RED}FAILED${NC} (JVM execution failed)"
            FAILED=$((FAILED + 1))
            continue
        fi
    fi

    # 比较输出（比较结果而不是完整输出）
    if [ "$native_result" = "$jvm_result" ]; then
        echo -e "${GREEN}✅ PASSED${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}❌ FAILED${NC} (output mismatch)"
        echo "  Native: $native_result"
        echo "  JVM:    $jvm_result"
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "=================================="
echo "Test Results"
echo "=================================="
echo -e "Total:  ${TOTAL}"
echo -e "Passed: ${GREEN}${PASSED}${NC}"
echo -e "Failed: ${RED}${FAILED}${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All golden tests passed!${NC}"
    exit 0
else
    echo -e "${RED}❌ Some tests failed!${NC}"
    exit 1
fi
