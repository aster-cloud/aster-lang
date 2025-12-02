#!/usr/bin/env bash
set -euo pipefail

say(){ printf "%s\n" "$*"; }
ok(){ printf "[OK] %s\n" "$*"; }
warn(){ printf "[WARN] %s\n" "$*" >&2; }
err(){ printf "[ERR] %s\n" "$*" >&2; exit 1; }

PROJECT_ROOT="$(pwd)"
CLAUDE_DIR="$PROJECT_ROOT/.claude"
CODEX_DIR="$PROJECT_ROOT/.codex"
SHRIMP_DIR="$PROJECT_ROOT/.shrimp"
TS="$(date -u +%Y%m%d-%H%M%S)"
TASK_MARKER="[TASK_MARKER: ${TS}-SETUP]"
LOG_FILE="$CLAUDE_DIR/operations-log.md"
SESS_FILE="$CLAUDE_DIR/codex-sessions.json"

# 创建必要的项目本地目录
mkdir -p "$CLAUDE_DIR" "$CODEX_DIR" "$SHRIMP_DIR"
[ -f "$LOG_FILE" ] || touch "$LOG_FILE"
[ -f "$SESS_FILE" ] || echo "[]" > "$SESS_FILE"

# 记录初始化操作到日志
cat >> "$LOG_FILE" <<LOG

---
## 初始化项目 AI 工作流 - ${TS}
- 执行者: setup-ai-workflow.sh
- 项目根目录: $PROJECT_ROOT
- Claude 配置目录: $CLAUDE_DIR
- Codex 配置目录: $CODEX_DIR
- Shrimp 数据目录: $SHRIMP_DIR

LOG

need(){ command -v "$1" >/dev/null 2>&1 || err "缺少依赖：$1"; }

say "== 0) 依赖体检 =="
need codex; ok "$(codex --version | head -n1)"
need node;  ok "node $(node -v)"
need npm;   ok "npm $(npm -v)"
if command -v uv >/dev/null 2>&1; then ok "$(uv --version | head -n1)"; else warn "未检测到 uv（可选）"; fi
if ! command -v jq >/dev/null 2>&1; then warn "未检测到 jq（可选）"; fi
if ! command -v script >/dev/null 2>&1; then warn "未检测到 script（仅在无 exec 时作为回退）"; fi

# 检查并安装 direnv
if ! command -v direnv >/dev/null 2>&1; then
  warn "未检测到 direnv，正在尝试安装..."
  if command -v brew >/dev/null 2>&1; then
    brew install direnv || warn "direnv 安装失败，请手动安装: brew install direnv"
  else
    warn "未检测到 Homebrew，请手动安装 direnv"
  fi
fi

if command -v direnv >/dev/null 2>&1; then
  ok "direnv $(direnv version)"
else
  warn "direnv 未安装，项目级 CODEX_HOME 将无法自动设置"
  warn "请手动设置 CODEX_HOME 为项目本地 .codex 目录, 例如: export CODEX_HOME=$CODEX_DIR"
fi

say "== 1) 写入项目本地 Claude Code 配置 =="
say "说明: 配置 6 个 MCP 服务器 - sequential-thinking, shrimp-task-manager, codex, chrome-devtools, exa, code-index"
# 项目本地配置（优先级高于全局）
cat > "$CLAUDE_DIR/config.json" <<'JSON'
{
  "mcpServers": {
    "sequential-thinking": { "type": "stdio", "command": "npx", "args": ["-y","@modelcontextprotocol/server-sequential-thinking"], "env": {} },
    "shrimp-task-manager": { "type": "stdio", "command": "npx", "args": ["-y","mcp-shrimp-task-manager"], "env": { "DATA_DIR": ".shrimp", "TEMPLATES_USE": "zh", "ENABLE_GUI": "false" } },
    "codex":                { "type": "stdio", "command": "codex", "args": ["mcp-server"], "env": {} },
    "chrome-devtools":     { "type": "stdio", "command": "npx", "args": ["chrome-devtools-mcp@latest"], "env": {} },
    "exa":                 { "type": "stdio", "command": "npx", "args": ["-y","exa-mcp-server"], "env": { "EXA_API_KEY": "8a356ce2-c28d-4465-85cc-58eca0574617" } },
    "code-index":          { "type": "stdio", "command": "uvx", "args": ["code-index-mcp"], "env": {} }
  }
}
JSON
ok "已写入项目本地配置: $CLAUDE_DIR/config.json"

# 写入 Codex 项目本地配置
say "说明: 配置 5 个 MCP 服务器 - shrimp-task-manager, chrome-devtools, sequential-thinking, code-index, exa"
CODEX_CONFIG="$CODEX_DIR/config.toml"

cat > "$CODEX_CONFIG" <<'TOML'
model = "gpt-5"
model_reasoning_effort = "low"

[mcp_servers.shrimp-task-manager]
command = "npx"
args = ["-y", "mcp-shrimp-task-manager"]
env = { DATA_DIR = ".shrimp", TEMPLATES_USE = "zh", ENABLE_GUI = "false" }

[mcp_servers.chrome-devtools]
command = "npx"
args = ["chrome-devtools-mcp@latest"]

[mcp_servers.sequential-thinking]
command = "npx"
args = ["-y", "@modelcontextprotocol/server-sequential-thinking"]

[mcp_servers.code-index]
command = "uvx"
args = ["code-index-mcp"]
startup_timeout_ms = 30000

[mcp_servers.exa]
command = "npx"
args = ["-y", "@smithery/cli@latest", "run", "exa", "--key", "8a356ce2-c28d-4465-85cc-58eca0574617"]
env = { EXA_API_KEY = "8a356ce2-c28d-4465-85cc-58eca0574617" }
TOML

ok "已写入项目本地 Codex 配置: $CODEX_CONFIG"

# 创建 .envrc 文件以自动设置 CODEX_HOME 和 CLAUDE_MCP_CONFIG
say "== 1.5) 配置 direnv 自动设置项目本地环境变量 =="
ENVRC_FILE="$PROJECT_ROOT/.envrc"
cat > "$ENVRC_FILE" <<'ENVRC'
export CODEX_HOME="$(pwd)/.codex"
export CLAUDE_MCP_CONFIG="$(pwd)/.claude/config.json"
ENVRC

ok "已创建 .envrc 文件: $ENVRC_FILE"
ok "  - CODEX_HOME 指向项目本地 .codex/"
ok "  - CLAUDE_MCP_CONFIG 指向项目本地 .claude/config.json"

# 允许 direnv 加载 .envrc
if command -v direnv >/dev/null 2>&1; then
  direnv allow "$PROJECT_ROOT" 2>/dev/null || warn "无法自动允许 direnv，请手动运行: direnv allow"
  ok "direnv 已允许加载 .envrc"
else
  warn "direnv 未安装，请安装后运行: direnv allow"
fi

# 记录配置操作到日志
cat >> "$LOG_FILE" <<LOG

### 配置文件写入完成
- Claude Config: $CLAUDE_DIR/config.json
  - MCP 服务器: sequential-thinking, shrimp-task-manager, codex, chrome-devtools, exa, code-index
- Codex Config: $CODEX_CONFIG
  - MCP 服务器: shrimp-task-manager, chrome-devtools, sequential-thinking, code-index, exa
  - 模型: gpt-5 (reasoning_effort=low)
- direnv 配置: $ENVRC_FILE
  - 自动设置 CODEX_HOME 为项目本地 .codex 目录
  - 自动设置 CLAUDE_MCP_CONFIG 为项目本地 .claude/config.json

LOG

say "== 2) 配置文件验证 =="
# 验证项目本地配置文件
if [ -f "$CLAUDE_DIR/config.json" ]; then
  ok "项目本地 Claude 配置: $CLAUDE_DIR/config.json"
else
  err "Claude 配置文件写入失败"
fi

if [ -f "$CODEX_CONFIG" ]; then
  ok "项目本地 Codex 配置: $CODEX_CONFIG"
else
  err "Codex 配置文件写入失败"
fi

# 验证 MCP 服务器配置（从项目配置文件）
for srv in sequential-thinking shrimp-task-manager codex chrome-devtools exa code-index; do
  if grep -q "\"$srv\"" "$CLAUDE_DIR/config.json" 2>/dev/null; then
    ok "MCP 配置已写入 Claude: $srv"
  else
    warn "MCP 配置缺失: $srv"
  fi
done

# 验证 Codex MCP 配置
for srv in chrome-devtools code-index sequential-thinking exa; do
  if grep -q "\[$srv\]" "$CODEX_CONFIG" 2>/dev/null || grep -q "mcp_servers.$srv" "$CODEX_CONFIG" 2>/dev/null; then
    ok "MCP 配置已写入 Codex: $srv"
  else
    warn "Codex MCP 配置缺失: $srv"
  fi
done

say "== 3) 工作流验证说明 =="
say ""
say "🎯 Smoke Test 任务: 使用 HTML 和 Three.js 创建一个 3D 科学计算器"
say ""
say "📋 正确的工作流启动方式："
say "  1. 在 Claude Code 中启动对话"
say "  2. 使用 /standard-workflow 或直接描述任务"
say "  3. Claude (主AI) 将自动执行 4 步流程："
say "     ① sequential-thinking - 深度思考设计方案"
say "     ② Codex 收集上下文 - 搜索 Three.js 最佳实践"
say "     ③ shrimp-task-manager - 任务拆解规划"
say "     ④ 主AI 编码 + Codex 审查 - 实现并验证"
say ""
say "✅ 验证产物（预期在 .claude/ 目录）："
say "  - context-smoke.json (步骤2: 技术参考和设计灵感)"
say "  - tasks-smoke.json (步骤3: 任务拆分计划)"
say "  - calculator-3d.html (步骤4: 完整的 3D 科学计算器)"
say ""
say "⚠️  注意事项："
say "  - 工作流起点是 Claude (主AI),而非直接 codex exec"
say "  - 必须遵循 5 条铁律 (详见 standard-workflow.md)"
say "  - 所有文件必须写入项目本地 .claude/ 目录"
say ""
say "📖 详细规范请参考："
say "  - standard-workflow.md (4 步执行流程)"
say "  - .claude/CLAUDE.md (主AI 工作规范)"
say "  - .codex/AGENTS.md (Codex 执行AI 规范)"
say ""

# 创建 smoke test 任务说明文件
SMOKE_TASK_FILE="$CLAUDE_DIR/smoke-test-task.md"
cat > "$SMOKE_TASK_FILE" <<'TASK'
# Smoke Test 任务说明

## 任务目标
使用 HTML 和 Three.js 创建一个单页面漂亮的 3D 科学计算器

## 功能需求
### 基础运算
- 加法 (+)
- 减法 (-)
- 乘法 (×)
- 除法 (÷)

### 科学函数
- 三角函数: sin, cos, tan
- 对数函数: log (常用对数), ln (自然对数)
- 指数函数: exp, pow
- 平方根: sqrt

### 3D 交互
- 鼠标悬停高亮效果
- 点击按钮动画
- 流畅的 3D 视角控制

### 界面美化
- 现代化配色方案
- 流畅的动画效果
- 3D 景深效果

## 技术栈
- HTML5
- Three.js (最新稳定版本)
- 原生 JavaScript (或 ES6+)

## 交付物
1. `.claude/context-smoke.json` - 上下文收集结果 (技术参考、设计灵感)
2. `.claude/tasks-smoke.json` - 任务规划结果 (shrimp-task-manager 生成)
3. `.claude/calculator-3d.html` - 完整的 3D 科学计算器单页面应用

## 启动方式
在 Claude Code 中直接说:
```
请按照 standard-workflow 执行 .claude/smoke-test-task.md 中的任务
```

或使用 slash 命令 (如果已配置):
```
/standard-workflow 实现 .claude/smoke-test-task.md 中的 3D 科学计算器
```

## 验证标准
- [ ] calculator-3d.html 文件已创建
- [ ] 包含 Three.js 引用和初始化代码
- [ ] 实现了所有基础运算功能
- [ ] 实现了所有科学函数
- [ ] 3D 交互流畅无卡顿
- [ ] 界面美观,符合现代设计标准
- [ ] 代码结构清晰,注释完整
TASK

ok "已创建 Smoke Test 任务说明: $SMOKE_TASK_FILE"
say ""

say "💡 如何开始 Smoke Test:"
say "   在 Claude Code 中执行以下命令:"
say "   \$ cat .claude/smoke-test-task.md"
say "   然后说: '请按照 standard-workflow 执行上述任务'"
say ""

# 最终总结到日志
cat >> "$LOG_FILE" <<LOG

---
## 设置完成 - ${TS}
- 项目目录: $PROJECT_ROOT
- Claude 配置: $CLAUDE_DIR/config.json
- Codex 配置: $CODEX_CONFIG
- 工作流规范遵循: sequential-thinking → shrimp-task-manager → Codex
- 路径规范: 所有工作文件写入 .claude/ (项目本地)

LOG

echo
echo "== 完成 =="
echo "• 项目目录：$PROJECT_ROOT"
echo "• Claude 配置（项目本地）：$CLAUDE_DIR/config.json"
echo "  - MCP: sequential-thinking, shrimp-task-manager, codex, chrome-devtools, exa, code-index"
echo "• Codex 配置（项目本地）：$CODEX_CONFIG"
echo "  - MCP: chrome-devtools, sequential-thinking, exa"
echo "• 操作日志：$LOG_FILE"
echo ""
echo "📋 工作流规范 (5条铁律):"
echo "  1. 操作前必须 sequential-thinking"
echo "  2. 上下文由 Codex 收集 → .claude/"
echo "  3. 路径必须 <project>/.claude/"
echo "  4. 默认自动执行（仅5类例外）"
echo "  5. 工具顺序: sequential-thinking → shrimp-task-manager → Codex"
echo ""
echo "🔧 配置说明:"
echo "  - 所有配置均为项目本地，不影响全局设置"
echo "  - Claude Code 通过 CLAUDE_MCP_CONFIG 环境变量读取项目本地 .claude/config.json"
echo "  - Codex 通过 CODEX_HOME 环境变量使用项目本地 .codex/config.toml"
echo "  - direnv 在进入项目目录时自动设置这些环境变量"
echo "  - 使用 Claude CLI 时需要: claude --mcp-config \$CLAUDE_MCP_CONFIG"
echo ""
echo "📖 参考文档:"
echo "  - 工作流标准: standard-workflow.md"
echo "  - 主AI规范: .claude/CLAUDE.md"
echo "  - Codex规范: .codex/AGENTS.md"
