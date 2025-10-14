# Aster Language VSCode Extension

Aster 语言的 Visual Studio Code 扩展，提供完整的语言支持。

## 功能特性

### 🎨 语法高亮
- 关键字、类型、函数名高亮
- 注释、字符串、数字识别
- 效果声明（@io, @cpu, @pure）特殊标记
- 能力前缀（Http, Db, Files 等）识别

### 🔍 LSP 集成
- **代码补全**：智能提示函数、类型、变量
- **跳转定义**：快速跳转到函数、类型定义
- **查找引用**：查找符号的所有使用位置
- **悬停提示**：显示类型信息和函数签名
- **错误检查**：实时类型检查和能力验证
- **重命名**：安全的符号重命名
- **代码格式化**：自动格式化 .cnl 文件
- **工作区符号**：跨文件符号搜索

### 📝 代码片段
提供 20+ 常用代码片段，包括：
- `module` - 模块声明
- `to` / `towith` / `toio` - 函数定义
- `data` / `enum` - 类型定义
- `if` / `match` - 控制流
- `httpget` / `dbquery` - 常用 IO 操作

## 安装要求

- Visual Studio Code >= 1.85.0
- Node.js >= 16
- Aster 语言项目（需要构建 LSP 服务器）

## 使用说明

### 1. 构建 LSP 服务器
在 Aster 项目根目录运行：
```bash
npm install
npm run build
```

这将在 `dist/src/lsp/server.js` 生成 LSP 服务器。

### 2. 安装扩展

**方式一：本地开发**
- 在 VSCode 中打开 `aster-vscode` 目录
- 按 `F5` 启动扩展开发主机

**方式二：VSIX 安装**
```bash
cd aster-vscode
npm install
npm run package
code --install-extension aster-vscode-0.1.0.vsix
```

### 3. 打开 Aster 项目
使用 VSCode 打开包含 `.cnl` 文件的文件夹，扩展会自动启动语言服务器。

## 配置选项

在 VSCode 设置中配置扩展（`.vscode/settings.json`）：

### 基础配置
```json
{
  "aster.langServer.path": "dist/src/lsp/server.js"
}
```

### 高级配置
```json
{
  "asterLanguageServer": {
    "index": {
      "persist": true,
      "path": ".asteri/lsp-index.json"
    },
    "format": {
      "mode": "lossless",
      "reflow": true
    },
    "rename": {
      "scope": "workspace"
    },
    "diagnostics": {
      "workspace": true
    }
  }
}
```

### 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `aster.langServer.path` | `"dist/src/lsp/server.js"` | LSP 服务器路径（相对于工作区根目录） |
| `asterLanguageServer.index.persist` | `true` | 是否持久化工作区符号索引 |
| `asterLanguageServer.index.path` | 无 | 自定义索引文件路径 |
| `asterLanguageServer.format.mode` | `"lossless"` | 格式化模式：`lossless` 或 `normalize` |
| `asterLanguageServer.format.reflow` | `true` | 允许在无损模式下最小化行调整 |
| `asterLanguageServer.rename.scope` | `"workspace"` | 重命名范围：`open`（仅打开文件）或 `workspace`（整个工作区） |
| `asterLanguageServer.diagnostics.workspace` | `true` | 启用工作区级别诊断 |

## 能力验证

Aster 支持基于能力清单（Capability Manifest）的权限检查。

### 配置能力清单

设置 `ASTER_CAPS` 环境变量指向清单文件：

```bash
# 启动 VSCode 时指定能力清单
ASTER_CAPS=cnl/examples/capabilities.json code .
```

### 清单格式

```json
{
  "allow": {
    "io": ["demo.app.*"],
    "cpu": ["*"]
  },
  "deny": {
    "io": ["demo.app.unsafe*"]
  }
}
```

### 模式语法
- `*` - 匹配所有
- `module` - 匹配具体模块
- `module.*` - 匹配模块下所有函数
- `module.func` - 匹配具体函数
- `module.func*` - 前缀匹配

## 命令

### Aster: Start Language Server
**命令 ID**: `aster.startLanguageServer`

手动启动语言服务器（通常会自动启动）。

**使用方式**:
1. 按 `Cmd+Shift+P` (macOS) 或 `Ctrl+Shift+P` (Windows/Linux)
2. 输入 "Aster: Start Language Server"
3. 回车执行

## 语法示例

```aster
This module is demo.app.

# 纯函数
To greet with name: Text, produce Text:
  Return Text.concat("Hello, ", name).

# IO 函数
To fetch_data, produce Text. It performs io:
  Return Http.get("/api/data").

# 数据类型
A User is a record of:
  It has name: Text.
  It has age: Int.

# 枚举类型
A Status is one of:
  Active.
  Inactive.
  Pending.
```

## 故障排除

### LSP 未找到错误
**错误信息**: "Aster LSP 未找到: dist/src/lsp/server.js。请先构建项目（npm run build）。"

**解决方案**:
1. 确认在项目根目录运行 `npm run build`
2. 检查 `dist/src/lsp/server.js` 文件是否存在
3. 确认 `aster.langServer.path` 配置正确

### 语言服务器无响应
**解决方案**:
1. 重启 VSCode
2. 查看输出面板：`View > Output` → 选择 "Aster Language Server"
3. 确认工作区中有 `.cnl` 文件
4. 检查 Node.js 版本 >= 16

### 语法高亮不工作
**解决方案**:
1. 确认文件扩展名为 `.cnl`
2. 手动设置语言模式：右下角点击语言 → 选择 "Aster"
3. 重新加载窗口：`Cmd/Ctrl+Shift+P` → "Developer: Reload Window"

### 代码片段无提示
**解决方案**:
1. 确认在 `.cnl` 文件中输入
2. 检查 VSCode 设置中 `editor.snippetSuggestions` 未设置为 `"none"`

## 开发指南

### 编译扩展
```bash
cd aster-vscode
npm install
npm run compile
```

### 监视模式
```bash
npm run watch
```

### 打包扩展
```bash
npm run package
```

生成 `.vsix` 文件，可分发安装。

### 调试扩展
1. 在 VSCode 中打开 `aster-vscode`
2. 按 `F5` 启动调试
3. 新窗口会加载扩展，可在原窗口设置断点

## 技术栈

- **语言**: TypeScript
- **LSP 客户端**: vscode-languageclient ^9.0.1
- **语法高亮**: TextMate Grammar (JSON)
- **构建工具**: TypeScript Compiler

## 许可证

MIT

## 作者

WontLost Ltd

## 反馈与支持

- 问题反馈：https://github.com/wontlost-ltd/aster-lang/issues
- 文档：https://github.com/wontlost-ltd/aster-lang
- 示例代码：`cnl/examples/`

## 更新日志

### 0.1.0 (2025-10-09)
- ✨ 首次发布
- ✅ TextMate 语法高亮
- ✅ LSP 完整集成
- ✅ 20+ 代码片段
- ✅ TypeScript 源码
- ✅ 能力验证支持
