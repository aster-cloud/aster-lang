#!/bin/bash
# Aster VSCode Extension 诊断脚本

set -e

echo "==================================="
echo "Aster VSCode Extension 诊断工具"
echo "==================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# 1. 检查 Node.js 版本
echo "1. 检查 Node.js 版本"
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    NODE_MAJOR=$(echo $NODE_VERSION | cut -d'.' -f1 | sed 's/v//')
    if [ "$NODE_MAJOR" -ge 16 ]; then
        check_pass "Node.js 版本: $NODE_VERSION (>= 16 ✓)"
    else
        check_fail "Node.js 版本: $NODE_VERSION (需要 >= 16)"
    fi
else
    check_fail "Node.js 未安装"
fi
echo ""

# 2. 检查 VSCode
echo "2. 检查 VSCode"
if command -v code &> /dev/null; then
    VSCODE_VERSION=$(code --version | head -1)
    check_pass "VSCode 已安装: $VSCODE_VERSION"
else
    check_warn "无法运行 'code' 命令（可能需要添加到 PATH）"
fi
echo ""

# 3. 检查扩展是否已安装
echo "3. 检查 Aster 扩展安装状态"
if command -v code &> /dev/null; then
    INSTALLED=$(code --list-extensions 2>/dev/null | grep -i aster || echo "")
    if [ -n "$INSTALLED" ]; then
        check_pass "扩展已安装: $INSTALLED"
    else
        check_fail "扩展未安装"
        echo "   请运行: code --install-extension aster-vscode-0.3.0.vsix --force"
    fi
else
    check_warn "跳过（code 命令不可用）"
fi
echo ""

# 4. 检查 VSIX 文件
echo "4. 检查 VSIX 文件"
VSIX_PATH="aster-vscode-0.3.0.vsix"
if [ -f "$VSIX_PATH" ]; then
    VSIX_SIZE=$(ls -lh "$VSIX_PATH" | awk '{print $5}')
    check_pass "VSIX 文件存在: $VSIX_SIZE"

    # 检查 VSIX 内容
    echo "   检查 VSIX 内容..."

    if unzip -l "$VSIX_PATH" | grep -q "extension/out/extension.js"; then
        check_pass "   - extension.js 存在"
    else
        check_fail "   - extension.js 缺失"
    fi

    if unzip -l "$VSIX_PATH" | grep -q "extension/dist/src/lsp/server.js"; then
        check_pass "   - LSP server.js 存在"
    else
        check_fail "   - LSP server.js 缺失"
    fi

    if unzip -l "$VSIX_PATH" | grep -q "extension/dist/scripts/aster.js"; then
        check_pass "   - CLI aster.js 存在"
    else
        check_fail "   - CLI aster.js 缺失"
    fi
else
    check_fail "VSIX 文件不存在: $VSIX_PATH"
    echo "   请运行: npm run package"
fi
echo ""

# 5. 检查源文件
echo "5. 检查源文件编译状态"
if [ -f "out/extension.js" ]; then
    check_pass "out/extension.js 已编译"
else
    check_fail "out/extension.js 未编译"
    echo "   请运行: npm run compile"
fi

if [ -f "dist/src/lsp/server.js" ]; then
    check_pass "dist/src/lsp/server.js 存在"
else
    check_fail "dist/src/lsp/server.js 缺失"
    echo "   请运行: cd .. && npm run build"
fi

if [ -f "dist/scripts/aster.js" ]; then
    check_pass "dist/scripts/aster.js 存在"
else
    check_fail "dist/scripts/aster.js 缺失"
    echo "   请运行: cd .. && npm run build"
fi
echo ""

# 6. 检查扩展安装目录（如果 code 可用）
echo "6. 检查 VSCode 扩展目录"
EXT_DIR="$HOME/.vscode/extensions"
if [ -d "$EXT_DIR" ]; then
    ASTER_EXT=$(find "$EXT_DIR" -maxdepth 1 -name "wontlost.aster-vscode-*" -type d 2>/dev/null || echo "")
    if [ -n "$ASTER_EXT" ]; then
        check_pass "扩展目录存在: $(basename "$ASTER_EXT")"

        # 检查关键文件
        if [ -f "$ASTER_EXT/out/extension.js" ]; then
            check_pass "   - extension.js 已安装"
        else
            check_fail "   - extension.js 缺失"
        fi

        if [ -f "$ASTER_EXT/dist/src/lsp/server.js" ]; then
            check_pass "   - LSP server.js 已安装"
        else
            check_fail "   - LSP server.js 缺失（需要重新安装）"
        fi
    else
        check_fail "扩展目录不存在"
        echo "   扩展可能未正确安装"
    fi
else
    check_warn "VSCode 扩展目录不存在: $EXT_DIR"
fi
echo ""

# 总结
echo "==================================="
echo "诊断完成"
echo "==================================="
echo ""
echo "如果发现问题，请按照以下步骤修复："
echo ""
echo "1. 卸载旧版本:"
echo "   code --uninstall-extension wontlost.aster-vscode"
echo ""
echo "2. 清理缓存（可选）:"
echo "   rm -rf ~/.vscode/extensions/wontlost.aster-vscode-*"
echo ""
echo "3. 重新构建和打包:"
echo "   npm run build:all"
echo "   npm run package"
echo ""
echo "4. 安装新版本:"
echo "   code --install-extension aster-vscode-0.3.0.vsix --force"
echo ""
echo "5. 完全重启 VSCode（Cmd+Q 退出，不是 Reload Window）"
echo ""
echo "详细故障排查指南: TROUBLESHOOTING.md"
echo ""
